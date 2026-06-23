package com.gdg.haksamo.domain.menu.dto;

import com.gdg.haksamo.domain.menu.entity.MealTime;

public record MenuResponse(
        String restaurant,
        String menuName,
        Integer price,
        String operatingTime,
        MealTime mealTime
) {

}