package com.gdg.haksamo.domain.review.repository;

import com.gdg.haksamo.domain.review.entity.ReviewHelpful;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewHelpfulRepository extends JpaRepository<ReviewHelpful, Long> {
    Optional<ReviewHelpful> findByReview_ReviewIdAndUserId(Long reviewId, Long userId);

    long countByReview_ReviewId(Long reviewId);
}
