package com.gdg.haksamo.domain.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자주 가는 식당")
public record FavoriteRestaurantResponse(

        @Schema(description = "식당 ID", example = "1")
        Long restaurantId,

        @Schema(description = "식당명", example = "정보센터")
        String name
) {
}
