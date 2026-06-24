package com.gdg.haksamo.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "리뷰 응답")
public record ReviewResponse(

        @Schema(description = "리뷰 ID")
        Long reviewId,

        @Schema(description = "작성자 ID (FE의 '내 리뷰' 판별용)")
        Long userId,

        @Schema(description = "작성자 닉네임", example = "학식러버")
        String authorNickname,

        @Schema(description = "식당명", example = "정보센터")
        String restaurant,

        @Schema(description = "메뉴명", example = "제육볶음")
        String menuName,

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
