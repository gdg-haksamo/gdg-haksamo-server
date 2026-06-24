package com.gdg.haksamo.domain.review.service;

import com.gdg.haksamo.domain.menu.entity.Menu;
import com.gdg.haksamo.domain.menu.repository.MenuRepository;
import com.gdg.haksamo.domain.review.dto.RatingDistributionResponse;
import com.gdg.haksamo.domain.review.dto.ReviewRequest;
import com.gdg.haksamo.domain.review.dto.ReviewResponse;
import com.gdg.haksamo.domain.review.entity.Review;
import com.gdg.haksamo.domain.review.entity.ReviewHelpful;
import com.gdg.haksamo.domain.review.repository.ReviewHelpfulRepository;
import com.gdg.haksamo.domain.review.repository.ReviewRepository;
import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import com.gdg.haksamo.global.security.RestaurantAdminGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private static final String UNKNOWN_AUTHOR = "알 수 없음";

    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulRepository reviewHelpfulRepository;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final RestaurantAdminGuard restaurantAdminGuard;

    @Transactional
    public ReviewResponse createReview(Long menuId, Long userId, ReviewRequest request) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        Review review = reviewRepository.save(
                Review.builder()
                        .menu(menu)
                        .userId(userId)
                        .rating(request.rating())
                        .content(request.content())
                        .build()
        );

        return toResponse(review, nicknamesOf(List.of(review)), helpfulCountsOf(List.of(review)));
    }

    public List<ReviewResponse> getReviews(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        return toResponses(reviewRepository.findByMenu(menu));
    }

    public List<ReviewResponse> getAllReviews() {
        return toResponses(reviewRepository.findAllByOrderByCreatedAtDesc());
    }

    public RatingDistributionResponse getRatingDistribution(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        Map<Integer, Long> distribution = new HashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            distribution.put(rating, 0L);
        }

        long total = 0;
        for (Object[] row : reviewRepository.countByRatingGroupedForMenu(menu)) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            distribution.put(rating, count);
            total += count;
        }

        return new RatingDistributionResponse(total, distribution);
    }

    @Transactional
    public void toggleHelpful(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 본인 리뷰에는 '도움됐어요'를 누를 수 없다(자기 리뷰 추천으로 통계 부풀리기 방지).
        if (review.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_HELPFUL_SELF_NOT_ALLOWED);
        }

        // 동시 토글로 인한 중복 insert는 UNIQUE(review_id, user_id) + 전역 핸들러(C003)로 방어한다.
        reviewHelpfulRepository.findByReview_ReviewIdAndUserId(reviewId, userId)
                .ifPresentOrElse(
                        reviewHelpfulRepository::delete,
                        () -> reviewHelpfulRepository.save(
                                ReviewHelpful.builder()
                                        .review(review)
                                        .userId(userId)
                                        .build()
                        )
                );
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 본인 리뷰면 삭제 허용. 본인이 아니면 관리자 권한 필요
        // (SUPER_ADMIN 전체 / RESTAURANT_ADMIN 본인 담당 식당 메뉴만). 그 외는 가드에서 403 A008.
        if (!review.getUserId().equals(userId)) {
            restaurantAdminGuard.requirePermission(userId, review.getMenu().getRestaurant().getRestaurantId());
        }

        reviewHelpfulRepository.deleteByReview_ReviewId(reviewId);
        reviewRepository.delete(review);
    }

    private List<ReviewResponse> toResponses(List<Review> reviews) {
        Map<Long, String> nicknames = nicknamesOf(reviews);
        Map<Long, Long> helpfulCounts = helpfulCountsOf(reviews);
        return reviews.stream()
                .map(review -> toResponse(review, nicknames, helpfulCounts))
                .toList();
    }

    /** 리뷰 작성자 userId → 닉네임 맵 (배치 조회로 N+1 회피). */
    private Map<Long, String> nicknamesOf(List<Review> reviews) {
        List<Long> userIds = reviews.stream().map(Review::getUserId).distinct().toList();
        Map<Long, String> nicknames = new HashMap<>();
        userRepository.findAllById(userIds)
                .forEach(user -> nicknames.put(user.getId(), user.getNickname()));
        return nicknames;
    }

    /** 리뷰 reviewId → 도움됐어요 수 맵 (배치 집계로 N+1 회피). */
    private Map<Long, Long> helpfulCountsOf(List<Review> reviews) {
        List<Long> reviewIds = reviews.stream().map(Review::getReviewId).toList();
        Map<Long, Long> counts = new HashMap<>();
        if (reviewIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : reviewHelpfulRepository.countByReviewIds(reviewIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private ReviewResponse toResponse(Review review, Map<Long, String> nicknames, Map<Long, Long> helpfulCounts) {
        long helpfulCount = helpfulCounts.getOrDefault(review.getReviewId(), 0L);

        return new ReviewResponse(
                review.getReviewId(),
                review.getUserId(),
                nicknames.getOrDefault(review.getUserId(), UNKNOWN_AUTHOR),
                review.getMenu().getRestaurant().getName(),
                review.getMenu().getName(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                helpfulCount
        );
    }
}
