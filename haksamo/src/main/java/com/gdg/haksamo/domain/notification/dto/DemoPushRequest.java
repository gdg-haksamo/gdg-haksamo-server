package com.gdg.haksamo.domain.notification.dto;

import com.gdg.haksamo.domain.menu.entity.MealTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 데모용 추천 푸시 요청 — 유저의 저장된 선호를 쓰지 않고, 입력값으로 추천을 생성한다.
 * (DB에 선호를 세팅하지 않고도 임의 시나리오를 시연하기 위함)
 */
public record DemoPushRequest(

        @Schema(description = "푸시를 받을 대상 사용자 id(이 사용자의 FCM 토큰으로 발송)", example = "1")
        @NotNull(message = "대상 사용자 id를 입력해주세요.")
        Long targetUserId,

        @Schema(description = "끼니", example = "LUNCH")
        @NotNull(message = "끼니(meal)를 입력해주세요.")
        MealTime meal,

        @Schema(description = "날짜(생략 시 오늘)", example = "2026-06-25")
        LocalDate date,

        @Schema(description = "선호 식당명 1개(선택)", example = "복지관")
        String favoriteRestaurant,

        @Schema(description = "선호 키워드 라벨 목록(선택)", example = "[\"매운 음식\", \"면류\"]")
        List<String> likedKeywords
) {
}
