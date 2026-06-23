package com.gdg.haksamo.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "인기 리뷰 (도움됐어요 많은 순)")
public record PopularReviewResponse(

        // TODO: User 엔티티 합류 시 작성자 닉네임 등으로 교체
        @Schema(description = "작성자 ID (임시, User 합류 전까지)", example = "1")
        Long userId,

        @Schema(description = "작성일시")
        LocalDateTime createdAt,

        @Schema(description = "별점 (1~5)", example = "5")
        Integer rating,

        @Schema(description = "리뷰 내용", example = "맛있어요")
        String content,

        @Schema(description = "도움됐어요 수", example = "3")
        long helpfulCount
) {
}
