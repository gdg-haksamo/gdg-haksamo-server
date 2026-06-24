package com.gdg.haksamo.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "리뷰 작성 요청")
public record ReviewRequest(

        @Schema(description = "별점 (1~5)", example = "5")
        @NotNull
        @Min(1)
        @Max(5)
        Integer rating,

        @Schema(description = "한줄평", example = "맛있어요")
        String content
) {
}
