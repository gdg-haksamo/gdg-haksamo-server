package com.gdg.haksamo.domain.review.controller;

import com.gdg.haksamo.domain.review.dto.RatingDistributionResponse;
import com.gdg.haksamo.domain.review.dto.ReviewRequest;
import com.gdg.haksamo.domain.review.dto.ReviewResponse;
import com.gdg.haksamo.domain.review.service.ReviewService;
import com.gdg.haksamo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "리뷰")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 작성", description = "메뉴에 별점/한줄평 리뷰를 작성합니다.")
    @PostMapping("/api/menus/{menuId}/reviews")
    public ApiResponse<ReviewResponse> createReview(
            @PathVariable Long menuId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.success(reviewService.createReview(menuId, userId, request));
    }

    @Operation(summary = "메뉴 리뷰 목록 조회", description = "메뉴의 리뷰 목록을 조회합니다.")
    @GetMapping("/api/menus/{menuId}/reviews")
    public ApiResponse<List<ReviewResponse>> getReviews(@PathVariable Long menuId) {
        return ApiResponse.success(reviewService.getReviews(menuId));
    }

    @Operation(summary = "전체 리뷰 목록 조회", description = "메뉴 구분 없이 전체 리뷰를 최신순으로 조회합니다.")
    @GetMapping("/api/reviews")
    public ApiResponse<List<ReviewResponse>> getAllReviews() {
        return ApiResponse.success(reviewService.getAllReviews());
    }

    @Operation(summary = "메뉴 별점 분포 조회", description = "메뉴의 1~5점 별점 분포를 조회합니다.")
    @GetMapping("/api/menus/{menuId}/rating-distribution")
    public ApiResponse<RatingDistributionResponse> getRatingDistribution(@PathVariable Long menuId) {
        return ApiResponse.success(reviewService.getRatingDistribution(menuId));
    }

    @Operation(summary = "리뷰 도움됐어요 토글", description = "이미 누른 상태면 취소, 안 누른 상태면 등록합니다.")
    @PostMapping("/api/reviews/{reviewId}/helpful")
    public ApiResponse<Void> toggleHelpful(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long userId
    ) {
        reviewService.toggleHelpful(reviewId, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "관리자 리뷰 삭제", description = "운영팀(SUPER_ADMIN)은 전체, 식당 운영자(RESTAURANT_ADMIN)는 본인 담당 식당 메뉴의 리뷰만 삭제합니다. 도움됐어요 기록도 함께 삭제됩니다.")
    @DeleteMapping("/api/reviews/{reviewId}")
    public ApiResponse<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long userId
    ) {
        reviewService.deleteReview(userId, reviewId);
        return ApiResponse.success();
    }
}
