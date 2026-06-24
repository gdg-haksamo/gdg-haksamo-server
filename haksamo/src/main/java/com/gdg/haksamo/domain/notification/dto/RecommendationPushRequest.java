package com.gdg.haksamo.domain.notification.dto;

import com.gdg.haksamo.domain.menu.entity.MealTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 관리자 추천 푸시 요청 — 대상 사용자 + 끼니(생략 시 현재 시각 기준).
 */
public record RecommendationPushRequest(

        @Schema(description = "푸시를 받을 대상 사용자 id", example = "1")
        @NotNull(message = "대상 사용자 id를 입력해주세요.")
        Long userId,

        @Schema(description = "끼니(BREAKFAST/LUNCH/DINNER, 생략 시 현재 시각 기준)", example = "LUNCH")
        MealTime meal
) {
}
