package com.gdg.haksamo.domain.menu.entity;

import java.time.LocalTime;

public enum MealTime {
    BREAKFAST("아침"),
    LUNCH("점심"),
    DINNER("저녁");

    private final String label;

    MealTime(String label) {
        this.label = label;
    }

    /** 사용자 노출용 한글 라벨 (예: 점심). */
    public String label() {
        return label;
    }

    /**
     * 시각으로 현재 끼니를 추정한다(끼니 미지정 조회/푸시의 기본값).
     * ~10:00 아침 / 10:00~15:00 점심 / 15:00~ 저녁.
     */
    public static MealTime fromTime(LocalTime time) {
        if (time.isBefore(LocalTime.of(10, 0))) {
            return BREAKFAST;
        }
        if (time.isBefore(LocalTime.of(15, 0))) {
            return LUNCH;
        }
        return DINNER;
    }
}
