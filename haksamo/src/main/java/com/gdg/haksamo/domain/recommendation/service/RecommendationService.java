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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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

    private final RecommendationRepository recommendationRepository;
    private final MenuScheduleRepository menuScheduleRepository;
    private final PreferenceRepository preferenceRepository;
    private final FavoriteRestaurantRepository favoriteRestaurantRepository;
    private final UserRepository userRepository;
    private final GeminiClient geminiClient;

    /**
     * 자기 자신 프록시. getToday를 비트랜잭션으로 두고 내부의 트랜잭션 단위(캐시조회/요청준비/저장)를
     * 프록시 경유로 호출해 각자 독립 트랜잭션을 갖게 한다. (같은 빈 내부의 직접 호출은 프록시를 우회해
     * @Transactional이 무시되므로 self 호출이 필요.) ObjectProvider로 생성 시점 순환 의존을 피한다.
     */
    private final ObjectProvider<RecommendationService> selfProvider;

    private RecommendationService self() {
        return selfProvider.getObject();
    }

    /** 끼니 추천 조회. meal이 null이면 현재 시각 기준 끼니. 없으면 생성(Gemini 1회), 있으면 캐시. */
    /**
     * 끼니 추천 조회. 캐시에 있으면 반환, 없으면 Gemini 1회 호출해 생성·저장.
     *
     * <p><b>트랜잭션 경계:</b> 이 메서드는 트랜잭션을 열지 않는다. 캐시조회·요청준비·저장은 각각
     * 독립 트랜잭션({@code self()} 프록시 호출)이고, 느린 외부 호출(Gemini)은 그 사이 <b>트랜잭션 밖</b>에서
     * 한다 — 응답 대기 동안 DB 커넥션을 점유하지 않기 위함(NotificationService와 동일 원칙).
     * 저장 자체는 한 트랜잭션으로 원자적이므로 "추천 없이 푸시"는 발생하지 않는다(푸시는 이 호출 성공 후에만).
     */
    public TodayRecommendationResponse getToday(Long userId, MealTime meal) {
        MealTime targetMeal = resolveMeal(meal);
        LocalDate today = LocalDate.now(KST);

        TodayRecommendationResponse cached = self().findCachedResponse(userId, today, targetMeal);
        if (cached != null) {
            return cached;
        }
        GeminiRecommendationRequest request = self().prepareUserRequest(userId, today, targetMeal);
        List<GeminiPick> picks = geminiClient.recommend(request); // ← 트랜잭션 밖 외부 호출
        try {
            return self().storeRecommendation(userId, today, targetMeal, request, picks);
        } catch (DataIntegrityViolationException e) {
            // 동시 첫 요청이 (user,date,meal) 유니크에 먼저 저장 → 저장 tx는 정상 롤백됨.
            // 비트랜잭션인 여기서 그쪽 결과를 캐시로 다시 읽어 반환(같은 tx에서 잡으면 rollback-only 위험).
            TodayRecommendationResponse winner = self().findCachedResponse(userId, today, targetMeal);
            if (winner != null) {
                return winner;
            }
            throw new BusinessException(ErrorCode.RECOMMENDATION_UNAVAILABLE);
        }
    }

    /** 캐시된 추천이 있으면 응답으로 변환(지연 로딩 위해 트랜잭션 안에서), 없으면 null. */
    @Transactional(readOnly = true)
    public TodayRecommendationResponse findCachedResponse(Long userId, LocalDate date, MealTime meal) {
        return recommendationRepository.findByUserIdAndDateAndMeal(userId, date, meal)
                .map(this::toResponse)
                .orElse(null);
    }

    /** 사용자 선호·후보 메뉴를 읽어 Gemini 요청 DTO를 만든다(필드를 모두 끌어와 지연 로딩을 트랜잭션 안에 가둔다). */
    @Transactional(readOnly = true)
    public GeminiRecommendationRequest prepareUserRequest(Long userId, LocalDate date, MealTime meal) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        List<Menu> candidates = mealCandidates(date, meal);
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_MENU_TO_RECOMMEND);
        }
        return buildRequest(meal, candidates, likedKeywords(user), favoriteRestaurantNames(user));
    }

    /**
     * Gemini 응답(picks)으로 추천을 조립·저장하고 응답으로 변환한다.
     * 외부 호출 사이 동시 요청이 먼저 저장했으면 그 결과를 반환해 중복 생성을 버린다.
     */
    @Transactional
    public TodayRecommendationResponse storeRecommendation(Long userId, LocalDate date, MealTime meal,
            GeminiRecommendationRequest request, List<GeminiPick> picks) {
        TodayRecommendationResponse alreadyStored = recommendationRepository
                .findByUserIdAndDateAndMeal(userId, date, meal)
                .map(this::toResponse)
                .orElse(null);
        if (alreadyStored != null) {
            return alreadyStored; // 동시 첫 요청이 먼저 저장 → 이미 만든 추천 재사용
        }
        // 요청에 담긴 후보(index→menuId)를 트랜잭션 안에서 managed 엔티티로 다시 로딩. menuId로 매칭해 순서 변동에 안전.
        Map<Long, Menu> byId = mealCandidates(date, meal).stream()
                .collect(Collectors.toMap(Menu::getMenuId, menu -> menu, (a, b) -> a));
        List<Menu> indexed = request.candidates().stream()
                .map(candidate -> byId.get(candidate.menuId()))
                .toList();

        Recommendation recommendation = Recommendation.create(userId, date, meal);
        assembleMenus(recommendation, picks, indexed);
        return toResponse(persist(recommendation));
    }

    /** 끼니 추천 새로고침. Gemini 재호출 없이 다음 후보로 포인터 이동(마지막 다음은 첫 후보로 순환, 한도 없음). */
    @Transactional
    public TodayRecommendationResponse refresh(Long userId, MealTime meal) {
        MealTime targetMeal = resolveMeal(meal);
        LocalDate today = LocalDate.now(KST);
        Recommendation recommendation = recommendationRepository
                .findByUserIdAndDateAndMeal(userId, today, targetMeal)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

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
        List<Menu> candidates = mealCandidates(date, meal);
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_MENU_TO_RECOMMEND);
        }
        // 데모는 단일 admin 트리거·저동시성이라 Gemini 호출을 트랜잭션 안에 둬도 무방하다(앱 경로와 달리 분리 안 함).
        List<GeminiPick> picks = geminiClient.recommend(buildRequest(meal, candidates, keywords, favorites));

        Recommendation recommendation = Recommendation.create(userId, date, meal);
        assembleMenus(recommendation, picks, candidates);
        recommendation = persist(recommendation);

        List<AdhocRecommendation> result = new ArrayList<>();
        for (RecommendationMenu rm : recommendation.getMenus()) {
            Menu menu = rm.getMenu();
            String restaurant = (menu.getRestaurant() != null) ? menu.getRestaurant().getName() : null;
            result.add(new AdhocRecommendation(rm.getDisplayOrder(), menu.getMenuId(), menu.getName(), restaurant));
        }
        return result;
    }

    /**
     * picks를 후보(index 정렬, null 허용)에 매칭해 추천 메뉴를 채운다.
     * "끼니별 {@link #SHORTLIST_SIZE}개" 상한을 강제하고, 후보 밖 index·중복·사라진 메뉴는 건너뛴다.
     */
    private void assembleMenus(Recommendation recommendation, List<GeminiPick> picks, List<Menu> indexed) {
        int order = 0;
        Set<Long> usedMenuIds = new HashSet<>();
        for (GeminiPick pick : picks) {
            if (recommendation.getMenus().size() >= SHORTLIST_SIZE) {
                break; // "끼니별 4개" 계약 강제 — 모델이 초과 응답을 줘도 상한에서 끊는다
            }
            if (pick.index() < 0 || pick.index() >= indexed.size()) {
                continue; // 모델이 후보 밖 index를 줘도 무시 → 없는 메뉴는 절대 저장되지 않음
            }
            Menu menu = indexed.get(pick.index());
            if (menu == null) {
                continue; // 후보에서 사라진 메뉴(품절 토글 등) → 건너뜀
            }
            if (!usedMenuIds.add(menu.getMenuId())) {
                continue;
            }
            recommendation.addMenu(RecommendationMenu.of(menu, order++));
        }
        // 모델이 중복·범위 밖 index를 섞어 4개를 못 채웠으면 남은 후보로 채운다(새로고침 후보 부족 방지).
        // 채움분은 picks 뒤에 붙으므로 1순위(노출/푸시 대상)는 그대로 모델 선택을 유지한다.
        for (Menu menu : indexed) {
            if (recommendation.getMenus().size() >= SHORTLIST_SIZE) {
                break;
            }
            if (menu == null || !usedMenuIds.add(menu.getMenuId())) {
                continue;
            }
            recommendation.addMenu(RecommendationMenu.of(menu, order++));
        }
    }

    /**
     * 추천 저장. 메뉴가 없으면 예외. (user,date,meal) 유니크 충돌 시 {@link DataIntegrityViolationException}이
     * 호출 트랜잭션 밖으로 전파돼 그 트랜잭션은 정상 롤백되고, 충돌 복구(기존 행 재사용)는 비트랜잭션
     * 오케스트레이터({@link #getToday})에서 한다. (같은 트랜잭션 안에서 잡으면 rollback-only가 돼
     * 커밋 시 UnexpectedRollbackException 위험.)
     */
    private Recommendation persist(Recommendation recommendation) {
        if (recommendation.getMenus().isEmpty()) {
            throw new BusinessException(ErrorCode.RECOMMENDATION_UNAVAILABLE);
        }
        return recommendationRepository.saveAndFlush(recommendation);
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
                recommendation.candidateCount());
    }
}
