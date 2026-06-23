package com.gdg.haksamo.domain.menu.dto;

import com.gdg.haksamo.domain.menu.entity.MealTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메뉴 조회 응답")
public record MenuResponse(

        @Schema(description = "식당명", example = "복지관")
        String restaurant,

        @Schema(description = "메뉴명", example = "제육볶음")
        String menuName,

        @Schema(description = "가격", example = "5500")
        Integer price,

        @Schema(description = "운영시간", example = "11:00~13:30")
        String operatingTime,

        @Schema(description = "식사 시간", example = "LUNCH")
        MealTime mealTime
) {
}