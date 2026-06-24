package com.gdg.haksamo.domain.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "식당 응답")
public record RestaurantResponse(

        @Schema(description = "식당 ID", example = "1")
        Long restaurantId,

        @Schema(description = "식당명", example = "정보센터")
        String name
) {
}
