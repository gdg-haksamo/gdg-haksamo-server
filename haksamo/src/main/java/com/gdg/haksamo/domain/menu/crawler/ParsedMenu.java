package com.gdg.haksamo.domain.menu.crawler;

import com.gdg.haksamo.domain.menu.entity.MealTime;

public record ParsedMenu (
        String name,
        Integer price,
        String operatingTime,
        MealTime time,
        int dayIndex
) {
}
