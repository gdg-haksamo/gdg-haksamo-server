package com.gdg.haksamo.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "리뷰 응답")
public record ReviewResponse(

        @Schema(description = "리뷰 ID")
        Long reviewId,

        @Schema(description = "작성자 ID")
        Long userId,

        @Schema(description = "별점 (1~5)")
        Integer rating,

        @Schema(description = "한줄평")
        String content,

        @Schema(description = "작성일시")
        LocalDateTime createdAt,

        @Schema(description = "도움됐어요 수")
        long helpfulCount
) {
}
