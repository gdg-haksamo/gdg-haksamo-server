package com.gdg.haksamo.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "인기 리뷰 (도움됐어요 많은 순)")
public record PopularReviewResponse(

        @Schema(description = "작성자 ID (FE의 '내 리뷰' 판별용)", example = "1")
        Long userId,

        @Schema(description = "작성자 닉네임", example = "학식러버")
        String authorNickname,

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
