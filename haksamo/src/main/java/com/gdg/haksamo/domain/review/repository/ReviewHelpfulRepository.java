package com.gdg.haksamo.domain.review.repository;

import com.gdg.haksamo.domain.review.entity.ReviewHelpful;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewHelpfulRepository extends JpaRepository<ReviewHelpful, Long> {
    Optional<ReviewHelpful> findByReview_ReviewIdAndUserId(Long reviewId, Long userId);

    long countByReview_ReviewId(Long reviewId);

    @Query("select rh.review.reviewId, count(rh) from ReviewHelpful rh where rh.review.reviewId in :reviewIds group by rh.review.reviewId")
    List<Object[]> countByReviewIds(@Param("reviewIds") List<Long> reviewIds);
}
