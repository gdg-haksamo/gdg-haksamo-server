package com.gdg.haksamo.domain.menu.service;

import com.gdg.haksamo.domain.menu.dto.MealMenusResponse;
import com.gdg.haksamo.domain.menu.dto.MenuDetailResponse;
import com.gdg.haksamo.domain.menu.dto.MenuResponse;
import com.gdg.haksamo.domain.menu.dto.MenusByMealTimeResponse;
import com.gdg.haksamo.domain.menu.dto.NutritionResponse;
import com.gdg.haksamo.domain.menu.dto.PopularReviewResponse;
import com.gdg.haksamo.domain.menu.entity.MealTime;
import com.gdg.haksamo.domain.menu.entity.Menu;
import com.gdg.haksamo.domain.menu.entity.MenuSchedule;
import com.gdg.haksamo.domain.menu.repository.MenuRepository;
import com.gdg.haksamo.domain.menu.repository.MenuScheduleRepository;
import com.gdg.haksamo.domain.review.entity.Review;
import com.gdg.haksamo.domain.review.repository.ReviewHelpfulRepository;
import com.gdg.haksamo.domain.review.repository.ReviewRepository;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final MenuRepository menuRepository;
    private final MenuScheduleRepository menuScheduleRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulRepository reviewHelpfulRepository;
    private final UserRepository userRepository;

    private static final String UNKNOWN_AUTHOR = "알 수 없음";

    public MenusByMealTimeResponse getMenus(LocalDate date) {
        List<MenuSchedule> schedules = menuScheduleRepository.findByDate(date);

        List<Long> menuIds = schedules.stream()
                .map(schedule -> schedule.getMenu().getMenuId())
                .distinct()
                .toList();

        Map<Long, Double> averageRatingByMenuId = new HashMap<>();
        for (Object[] row : reviewRepository.averageRatingByMenuIds(menuIds)) {
            averageRatingByMenuId.put((Long) row[0], roundRating((Double) row[1]));
        }

        Map<MealTime, List<MenuResponse>> grouped = new HashMap<>();
        for (MealTime mealTime : MealTime.values()) {
            grouped.put(mealTime, new ArrayList<>());
        }

        for (MenuSchedule schedule : schedules) {
            grouped.get(schedule.getTime()).add(
                    new MenuResponse(
                            schedule.getMenu().getRestaurant().getName(),
                            schedule.getMenu().getName(),
                            schedule.getMenu().getPrice(),
                            averageRatingByMenuId.get(schedule.getMenu().getMenuId()),
                            schedule.isSoldOut()
                    )
            );
        }

        return new MenusByMealTimeResponse(
                MealMenusResponse.of(grouped.get(MealTime.BREAKFAST)),
                MealMenusResponse.of(grouped.get(MealTime.LUNCH)),
                MealMenusResponse.of(grouped.get(MealTime.DINNER))
        );
    }

    public MenuDetailResponse getMenuDetail(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        MenuSchedule schedule = menuScheduleRepository.findFirstByMenu(menu).orElse(null);
        String operatingTime = schedule != null ? schedule.getOperatingTime() : null;
        boolean soldOut = schedule != null && schedule.isSoldOut();

        Double averageRating = roundRating(reviewRepository.averageRatingForMenu(menu));
        long reviewCount = reviewRepository.countByMenu(menu);

        List<PopularReviewResponse> popularReviews = getPopularReviews(menu);

        return new MenuDetailResponse(
                menu.getName(),
                menu.getPrice(),
                menu.getRestaurant().getName(),
                operatingTime,
                soldOut,
                menu.getDescription(),
                menu.getImageUrl(),
                new NutritionResponse(
                        menu.getCalories(),
                        menu.getProtein(),
                        menu.getCarb(),
                        menu.getFat()
                ),
                averageRating,
                reviewCount,
                popularReviews
        );
    }

    private List<PopularReviewResponse> getPopularReviews(Menu menu) {
        List<Review> reviews = reviewRepository.findByMenu(menu);
        if (reviews.isEmpty()) {
            return List.of();
        }

        List<Long> reviewIds = reviews.stream().map(Review::getReviewId).toList();

        Map<Long, Long> helpfulCountByReviewId = new HashMap<>();
        for (Object[] row : reviewHelpfulRepository.countByReviewIds(reviewIds)) {
            helpfulCountByReviewId.put((Long) row[0], (Long) row[1]);
        }

        // 작성자 닉네임 배치 조회 (N+1 회피)
        List<Long> userIds = reviews.stream().map(Review::getUserId).distinct().toList();
        Map<Long, String> nicknameByUserId = new HashMap<>();
        userRepository.findAllById(userIds)
                .forEach(user -> nicknameByUserId.put(user.getId(), user.getNickname()));

        return reviews.stream()
                .sorted(Comparator
                        .comparingLong((Review r) -> helpfulCountByReviewId.getOrDefault(r.getReviewId(), 0L))
                        .reversed()
                        .thenComparing(Review::getCreatedAt, Comparator.reverseOrder()))
                .limit(3)
                .map(review -> new PopularReviewResponse(
                        review.getUserId(),
                        nicknameByUserId.getOrDefault(review.getUserId(), UNKNOWN_AUTHOR),
                        review.getCreatedAt(),
                        review.getRating(),
                        review.getContent(),
                        helpfulCountByReviewId.getOrDefault(review.getReviewId(), 0L)
                ))
                .toList();
    }

    private static Double roundRating(Double rating) {
        if (rating == null) {
            return null;
        }
        return Math.round(rating * 10) / 10.0;
    }
}
