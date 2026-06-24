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
import com.gdg.haksamo.domain.user.entity.Role;
import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulRepository reviewHelpfulRepository;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;

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

        return toResponse(review);
    }

    public List<ReviewResponse> getReviews(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        return reviewRepository.findByMenu(menu)
                .stream()
                .map(this::toResponse)
                .toList();
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
        if (!reviewRepository.existsById(reviewId)) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }

        reviewHelpfulRepository.findByReview_ReviewIdAndUserId(reviewId, userId)
                .ifPresentOrElse(
                        reviewHelpfulRepository::delete,
                        () -> reviewHelpfulRepository.save(
                                ReviewHelpful.builder()
                                        .review(reviewRepository.getReferenceById(reviewId))
                                        .userId(userId)
                                        .build()
                        )
                );
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == Role.USER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        reviewHelpfulRepository.deleteByReview_ReviewId(reviewId);
        reviewRepository.delete(review);
    }

    private ReviewResponse toResponse(Review review) {
        long helpfulCount = reviewHelpfulRepository.countByReview_ReviewId(review.getReviewId());

        return new ReviewResponse(
                review.getReviewId(),
                review.getUserId(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                helpfulCount
        );
    }
}
