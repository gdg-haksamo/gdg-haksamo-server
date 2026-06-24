package com.gdg.haksamo.domain.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "자주 가는 식당 수정 요청 (전체 교체)")
public record FavoriteRestaurantsUpdateRequest(

        @Schema(description = "자주 가는 식당 ID 목록 (이 목록으로 전체 교체됨)", example = "[1,3]")
        @NotNull
        List<Long> restaurantIds
) {
}
