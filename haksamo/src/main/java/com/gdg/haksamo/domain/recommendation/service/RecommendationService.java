package com.gdg.haksamo.domain.recommendation.service;

import com.gdg.haksamo.domain.menu.dto.NutritionResponse;
import com.gdg.haksamo.domain.menu.entity.MealTime;
import com.gdg.haksamo.domain.menu.entity.Menu;
import com.gdg.haksamo.domain.menu.entity.MenuSchedule;
import com.gdg.haksamo.domain.menu.repository.MenuScheduleRepository;
import com.gdg.haksamo.domain.preference.PreferenceRepository;
import com.gdg.haksamo.domain.recommendation.dto.AdhocRecommendation;
import com.gdg.haksamo.domain.recommendation.dto.TodayRecommendationResponse;
import com.gdg.haksamo.domain.recommendation.entity.Recommendation;
import com.gdg.haksamo.domain.recommendation.entity.RecommendationMenu;
import com.gdg.haksamo.domain.recommendation.gemini.GeminiCandidate;
import com.gdg.haksamo.domain.recommendation.gemini.GeminiClient;
import com.gdg.haksamo.domain.recommendation.gemini.GeminiPick;
import com.gdg.haksamo.domain.recommendation.gemini.GeminiRecommendationRequest;
import com.gdg.haksamo.domain.recommendation.repository.RecommendationRepository;
import com.gdg.haksamo.domain.restaurant.FavoriteRestaurant;
import com.gdg.haksamo.domain.restaurant.FavoriteRestaurantRepository;
import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalTime;
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
 * 끼니별 AI 추천 조회·새로고침.
 *
 * <p>추천은 (user, date, meal) 단위다. 끼니별 첫 조회 때 Gemini를 1회 호출해 후보 {@link #SHORTLIST_SIZE}개를
 * 미리 받아 저장하고, 같은 끼니 재조회는 캐시 반환, 새로고침은 미리 받아둔 다음 후보를 DB에서 꺼낸다.
 *
 * <p>프롬프트에는 <b>끼니 · 선호 키워드 · 선호 식당(과 그 식당 메뉴 표시)</b>를 반영한다. 사용자 규모가
 * 작아 끼니마다 per-user 생성한다(서명 dedup은 규모 확장 시 — docs/adr/0001).
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    /** 한국 시간 기준(서버 타임존이 UTC여도 날짜·끼니 경계를 KST로 맞춘다) */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int SHORTLIST_SIZE = 4;
    private static final int DAILY_REFRESH_LIMIT = 3;

    private final RecommendationRepository recommendationRepository;
    private final MenuScheduleRepository menuScheduleRepository;
    private final PreferenceRepository preferenceRepository;
    private final FavoriteRestaurantRepository favoriteRestaurantRepository;
    private final UserRepository userRepository;
    private final GeminiClient geminiClient;

    /** 끼니 추천 조회. meal이 null이면 현재 시각 기준 끼니. 없으면 생성(Gemini 1회), 있으면 캐시. */
    @Transactional
    public TodayRecommendationResponse getToday(Long userId, MealTime meal) {
        MealTime targetMeal = resolveMeal(meal);
        LocalDate today = LocalDate.now(KST);
        Recommendation recommendation = recommendationRepository
                .findByUserIdAndDateAndMeal(userId, today, targetMeal)
                .orElseGet(() -> generate(userId, today, targetMeal));
        return toResponse(recommendation);
    }

    /** 끼니 추천 새로고침. Gemini 재호출 없이 다음 후보로 포인터 이동. */
    @Transactional
    public TodayRecommendationResponse refresh(Long userId, MealTime meal) {
        MealTime targetMeal = resolveMeal(meal);
        LocalDate today = LocalDate.now(KST);
        Recommendation recommendation = recommendationRepository
                .findByUserIdAndDateAndMeal(userId, today, targetMeal)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        if (!recommendation.canRefresh(DAILY_REFRESH_LIMIT)) {
            throw new BusinessException(ErrorCode.RECOMMENDATION_REFRESH_LIMIT);
        }
        recommendation.refresh();
        return toResponse(recommendation);
    }

    private MealTime resolveMeal(MealTime meal) {
        return (meal != null) ? meal : MealTime.fromTime(LocalTime.now(KST));
    }

    /**
     * 데모용: 입력(날짜·끼니·선호 키워드·선호 식당)으로 추천 4개를 생성하고 <b>저장한다</b>(시연 새로고침이 동작하도록).
     * 유저의 저장된 선호와 무관하게 임의 시나리오를 구성한다. 같은 (user,date,meal) 추천이 있으면 교체한다.
     */
    @Transactional
    public List<AdhocRecommendation> generateForDemo(Long userId, LocalDate date, MealTime meal,
            List<String> likedKeywords, List<String> favoriteRestaurants) {
        // 시연 재실행 대비 — 기존 추천을 지우고 새로 만든다. (delete를 INSERT 전에 flush해 유니크 충돌 방지)
        recommendationRepository.findByUserIdAndDateAndMeal(userId, date, meal)
                .ifPresent(recommendationRepository::delete);
        recommendationRepository.flush();

        List<String> keywords = (likedKeywords != null) ? likedKeywords : List.of();
        List<String> favorites = (favoriteRestaurants != null) ? favoriteRestaurants : List.of();
        Recommendation recommendation = buildAndStore(userId, date, meal, keywords, favorites);

        List<AdhocRecommendation> result = new ArrayList<>();
        for (RecommendationMenu rm : recommendation.getMenus()) {
            Menu menu = rm.getMenu();
            String restaurant = (menu.getRestaurant() != null) ? menu.getRestaurant().getName() : null;
            result.add(new AdhocRecommendation(rm.getDisplayOrder(), menu.getMenuId(), menu.getName(), restaurant));
        }
        return result;
    }

    /** 앱 경로: 사용자의 저장된 선호(키워드·식당)로 생성·저장. */
    private Recommendation generate(Long userId, LocalDate today, MealTime meal) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return buildAndStore(userId, today, meal, likedKeywords(user), favoriteRestaurantNames(user));
    }

    /** 후보 조회 → Gemini로 4개 선택 → (user,date,meal) 추천 + RecommendationMenu 저장. */
    private Recommendation buildAndStore(Long userId, LocalDate date, MealTime meal,
            List<String> likedKeywords, List<String> favoriteRestaurants) {
        List<Menu> candidates = mealCandidates(date, meal);
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_MENU_TO_RECOMMEND);
        }
        List<GeminiPick> picks = geminiClient.recommend(buildRequest(meal, candidates, likedKeywords, favoriteRestaurants));

        Recommendation recommendation = Recommendation.create(userId, date, meal);
        int order = 0;
        Set<Long> usedMenuIds = new HashSet<>();
        for (GeminiPick pick : picks) {
            if (recommendation.getMenus().size() >= SHORTLIST_SIZE) {
                break; // "끼니별 4개" 계약 강제 — 모델이 초과 응답을 줘도 상한에서 끊는다
            }
            if (pick.index() < 0 || pick.index() >= candidates.size()) {
                continue; // 모델이 후보 밖 index를 줘도 무시 → 없는 메뉴는 절대 저장되지 않음
            }
            Menu menu = candidates.get(pick.index());
            if (!usedMenuIds.add(menu.getMenuId())) {
                continue;
            }
            recommendation.addMenu(RecommendationMenu.of(menu, order++));
        }
        if (recommendation.getMenus().isEmpty()) {
            throw new BusinessException(ErrorCode.RECOMMENDATION_UNAVAILABLE);
        }

        try {
            return recommendationRepository.saveAndFlush(recommendation);
        } catch (DataIntegrityViolationException e) {
            // 동시 첫 요청 → (user_id, date, meal) 유니크 충돌. 먼저 저장된 행 재사용.
            return recommendationRepository.findByUserIdAndDateAndMeal(userId, date, meal)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_UNAVAILABLE));
        }
    }

    /** 해당 끼니 후보 메뉴: 품절 제외, menuId 기준 중복 제거(편성 순서 유지). */
    private List<Menu> mealCandidates(LocalDate today, MealTime meal) {
        Map<Long, Menu> distinct = new LinkedHashMap<>();
        for (MenuSchedule schedule : menuScheduleRepository.findByDateAndTime(today, meal)) {
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

    /** 사용자 선호 키워드(LIKED) 라벨. */
    private List<String> likedKeywords(User user) {
        return preferenceRepository.findByUser(user).stream()
                .map(preference -> preference.getKeyword().getLabel())
                .toList();
    }

    /** 사용자 선호 식당명. */
    private List<String> favoriteRestaurantNames(User user) {
        return favoriteRestaurantRepository.findByUser(user).stream()
                .map(FavoriteRestaurant::getRestaurant)
                .filter(restaurant -> restaurant != null)
                .map(restaurant -> restaurant.getName())
                .toList();
    }

    private GeminiRecommendationRequest buildRequest(
            MealTime meal, List<Menu> candidates, List<String> likedKeywords, List<String> favoriteRestaurants) {
        Set<String> favoriteSet = new HashSet<>(favoriteRestaurants);
        List<GeminiCandidate> items = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            Menu menu = candidates.get(i);
            String restaurant = (menu.getRestaurant() != null) ? menu.getRestaurant().getName() : null;
            boolean favorite = restaurant != null && favoriteSet.contains(restaurant);
            items.add(new GeminiCandidate(
                    i, menu.getMenuId(), menu.getName(), restaurant, favorite,
                    menu.getCalories(), menu.getProtein(), menu.getCarb(), menu.getFat()));
        }
        return new GeminiRecommendationRequest(meal.label(), items, likedKeywords, favoriteRestaurants, SHORTLIST_SIZE);
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
                recommendation.getDate(),
                recommendation.getMeal(),
                recommendation.getRefreshCount(),
                Math.max(remaining, 0));
    }
}
