package com.gdg.haksamo.domain.recommendation.service;

import com.gdg.haksamo.domain.menu.dto.NutritionResponse;
import com.gdg.haksamo.domain.menu.entity.Menu;
import com.gdg.haksamo.domain.menu.entity.MenuSchedule;
import com.gdg.haksamo.domain.menu.repository.MenuScheduleRepository;
import com.gdg.haksamo.domain.preference.PreferenceRepository;
import com.gdg.haksamo.domain.recommendation.dto.TodayRecommendationResponse;
import com.gdg.haksamo.domain.recommendation.entity.Recommendation;
import com.gdg.haksamo.domain.recommendation.entity.RecommendationMenu;
import com.gdg.haksamo.domain.recommendation.gemini.GeminiCandidate;
import com.gdg.haksamo.domain.recommendation.gemini.GeminiClient;
import com.gdg.haksamo.domain.recommendation.gemini.GeminiPick;
import com.gdg.haksamo.domain.recommendation.gemini.GeminiRecommendationRequest;
import com.gdg.haksamo.domain.recommendation.repository.RecommendationRepository;
import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 오늘의 AI 추천 조회·새로고침.
 *
 * <p>핵심 비용 전략: 하루 첫 조회 때 Gemini를 <b>딱 1회</b> 호출해 추천 후보 {@link #SHORTLIST_SIZE}개를
 * 미리 받아 저장한다. 이후 같은 날 재조회는 캐시 반환, 새로고침은 미리 받아둔 다음 후보를
 * DB에서 꺼내 반환한다(Gemini 재호출 0 → 실시간·무비용). 사용자당 하루 Gemini 호출은 1회로 고정.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    /** 한국 시간 기준 "오늘". (EC2 등 서버 타임존이 UTC여도 날짜 경계를 KST로 맞춘다) */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 첫 호출 1회로 미리 받아둘 추천 개수(첫 화면 1 + 새로고침 3). */
    private static final int SHORTLIST_SIZE = 4;

    /** 하루 새로고침 한도. */
    private static final int DAILY_REFRESH_LIMIT = 3;

    private final RecommendationRepository recommendationRepository;
    private final MenuScheduleRepository menuScheduleRepository;
    private final PreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final GeminiClient geminiClient;

    /** 오늘 추천 조회. 없으면 생성(Gemini 1회), 있으면 캐시 반환. */
    @Transactional
    public TodayRecommendationResponse getToday(Long userId) {
        LocalDate today = LocalDate.now(KST);
        Recommendation recommendation = recommendationRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> generate(userId, today));
        return toResponse(recommendation);
    }

    /** 새로고침. Gemini 재호출 없이 미리 받아둔 다음 후보로 포인터를 옮긴다. */
    @Transactional
    public TodayRecommendationResponse refresh(Long userId) {
        LocalDate today = LocalDate.now(KST);
        Recommendation recommendation = recommendationRepository.findByUserIdAndDate(userId, today)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        if (!recommendation.canRefresh(DAILY_REFRESH_LIMIT)) {
            throw new BusinessException(ErrorCode.RECOMMENDATION_REFRESH_LIMIT);
        }
        recommendation.refresh();
        return toResponse(recommendation);
    }

    private Recommendation generate(Long userId, LocalDate today) {
        List<Menu> candidates = todayCandidates(today);
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_MENU_TO_RECOMMEND);
        }

        List<String> likedKeywords = likedKeywords(userId);
        List<GeminiPick> picks = geminiClient.recommend(buildRequest(candidates, likedKeywords));

        Recommendation recommendation = Recommendation.create(userId, today);
        int order = 0;
        Set<Long> usedMenuIds = new HashSet<>();
        for (GeminiPick pick : picks) {
            if (pick.index() < 0 || pick.index() >= candidates.size()) {
                continue; // 모델이 잘못된 index를 주면 건너뜀
            }
            Menu menu = candidates.get(pick.index());
            if (!usedMenuIds.add(menu.getMenuId())) {
                continue; // 같은 메뉴 중복 추천 방지
            }
            recommendation.addMenu(RecommendationMenu.of(menu, order++, pick.reason()));
        }
        if (recommendation.getMenus().isEmpty()) {
            throw new BusinessException(ErrorCode.RECOMMENDATION_UNAVAILABLE);
        }

        try {
            return recommendationRepository.saveAndFlush(recommendation);
        } catch (DataIntegrityViolationException e) {
            // 같은 사용자의 첫 요청이 거의 동시에 들어와 (user_id, date) 유니크 충돌 →
            // 먼저 저장된 행을 재사용한다.
            return recommendationRepository.findByUserIdAndDate(userId, today)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_UNAVAILABLE));
        }
    }

    /** 오늘 후보 메뉴: 품절 제외, menuId 기준 중복 제거(편성 순서 유지). */
    private List<Menu> todayCandidates(LocalDate today) {
        Map<Long, Menu> distinct = new LinkedHashMap<>();
        for (MenuSchedule schedule : menuScheduleRepository.findByDate(today)) {
            if (schedule.isSoldOut()) {
                continue;
            }
            Menu menu = schedule.getMenu();
            if (menu != null) {
                distinct.putIfAbsent(menu.getMenuId(), menu);
            }
        }
        return new ArrayList<>(distinct.values());
    }

    /** 사용자 선호 키워드(LIKED) 라벨. 기피(DISLIKED)는 데이터 모델에 없음(#257). */
    private List<String> likedKeywords(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return preferenceRepository.findByUser(user).stream()
                .map(preference -> preference.getKeyword().getLabel())
                .toList();
    }

    private GeminiRecommendationRequest buildRequest(List<Menu> candidates, List<String> likedKeywords) {
        List<GeminiCandidate> items = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            Menu menu = candidates.get(i);
            String restaurant = (menu.getRestaurant() != null) ? menu.getRestaurant().getName() : null;
            items.add(new GeminiCandidate(
                    i, menu.getMenuId(), menu.getName(), restaurant,
                    menu.getCalories(), menu.getProtein(), menu.getCarb(), menu.getFat()));
        }
        return new GeminiRecommendationRequest(items, likedKeywords, SHORTLIST_SIZE);
    }

    private TodayRecommendationResponse toResponse(Recommendation recommendation) {
        RecommendationMenu current = recommendation.currentMenu();
        Menu menu = current.getMenu();
        String restaurant = (menu.getRestaurant() != null) ? menu.getRestaurant().getName() : null;
        int remaining = recommendation.maxRefresh(DAILY_REFRESH_LIMIT) - recommendation.getRefreshCount();
        return new TodayRecommendationResponse(
                menu.getMenuId(),
                menu.getName(),
                restaurant,
                menu.getPrice(),
                menu.getCategory(),
                menu.getImageUrl(),
                menu.getDescription(),
                new NutritionResponse(menu.getCalories(), menu.getProtein(), menu.getCarb(), menu.getFat()),
                current.getReason(),
                recommendation.getDate(),
                recommendation.getRefreshCount(),
                Math.max(remaining, 0));
    }
}
