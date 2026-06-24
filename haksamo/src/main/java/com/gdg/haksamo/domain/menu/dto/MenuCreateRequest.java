package com.gdg.haksamo.domain.menu.dto;

import com.gdg.haksamo.domain.menu.entity.MealTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 신메뉴 등록 요청. Menu(카탈로그) + 당일 MenuSchedule(편성)을 함께 생성한다.
 * 권한은 서비스에서 RestaurantAdminGuard로 검증(본인 담당 식당만).
 */
public record MenuCreateRequest(

        @Schema(description = "메뉴를 등록할 식당 id", example = "1")
        @NotNull(message = "식당 id를 입력해주세요.")
        Long restaurantId,

        @Schema(description = "메뉴 이름", example = "제육덮밥")
        @NotBlank(message = "메뉴 이름을 입력해주세요.")
        @Size(max = 100, message = "메뉴 이름은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "가격(원)", example = "5000")
        @NotNull(message = "가격을 입력해주세요.")
        @PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
        Integer price,

        @Schema(description = "분류(선택)", example = "백반")
        String category,

        @Schema(description = "끼니(BREAKFAST/LUNCH/DINNER)", example = "LUNCH")
        @NotNull(message = "끼니(time)를 입력해주세요.")
        MealTime time
) {
}
