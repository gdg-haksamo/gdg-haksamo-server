package com.gdg.haksamo.domain.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "식당별 메뉴 목록 항목 (리뷰 작성용 메뉴 선택)")
public record RestaurantMenuResponse(

        @Schema(description = "메뉴 ID", example = "78")
        Long menuId,

        @Schema(description = "메뉴명", example = "제육볶음")
        String name,

        @Schema(description = "가격", example = "5500")
        Integer price
) {
}
