package com.gdg.haksamo.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "리뷰 작성 요청")
public record ReviewRequest(

        // TODO: User 엔티티/JWT 합류 시 제거하고 SecurityContext에서 추출
        @Schema(description = "작성자 ID (임시, JWT 합류 전까지 직접 전달)", example = "1")
        @NotNull
        Long userId,

        @Schema(description = "별점 (1~5)", example = "5")
        @NotNull
        @Min(1)
        @Max(5)
        Integer rating,

        @Schema(description = "한줄평", example = "맛있어요")
        String content
) {
}
