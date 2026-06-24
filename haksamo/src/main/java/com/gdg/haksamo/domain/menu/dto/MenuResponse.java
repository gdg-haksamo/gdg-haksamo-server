package com.gdg.haksamo.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메뉴 조회 응답")
public record MenuResponse(

        @Schema(description = "식당명", example = "복지관")
        String restaurant,

        @Schema(description = "메뉴명", example = "제육볶음")
        String menuName,

        @Schema(description = "가격", example = "5500")
        Integer price,

        @Schema(description = "평균 별점 (리뷰 없으면 null)", example = "4.3")
        Double averageRating,

        @Schema(description = "품절 여부", example = "false")
        boolean soldOut
) {
}