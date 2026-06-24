package com.gdg.haksamo.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "끼니별로 묶은 날짜별 메뉴 조회 응답")
public record MenusByMealTimeResponse(

        @Schema(description = "아침 메뉴")
        MealMenusResponse breakfast,

        @Schema(description = "점심 메뉴")
        MealMenusResponse lunch,

        @Schema(description = "저녁 메뉴")
        MealMenusResponse dinner
) {
}
