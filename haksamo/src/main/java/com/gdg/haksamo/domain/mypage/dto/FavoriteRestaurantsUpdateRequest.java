package com.gdg.haksamo.domain.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자주 가는 식당 수정 요청 (단일, 전체 교체)")
public record FavoriteRestaurantsUpdateRequest(

        @Schema(description = "자주 가는 식당 ID (단일). null이면 선호 식당 해제", example = "1")
        Long restaurantId
) {
}
