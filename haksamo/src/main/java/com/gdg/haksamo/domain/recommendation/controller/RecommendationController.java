package com.gdg.haksamo.domain.recommendation.controller;

import com.gdg.haksamo.domain.menu.entity.MealTime;
import com.gdg.haksamo.domain.recommendation.dto.TodayRecommendationResponse;
import com.gdg.haksamo.domain.recommendation.service.RecommendationService;
import com.gdg.haksamo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 오늘의 AI 학식 추천(홈 화면 메인 카드). 로그인 필요(비로그인 401 → FE 로그인 유도).
 */
@Tag(name = "Recommendation - AI 학식 추천", description = "Gemini 기반 오늘의 학식 추천 조회/새로고침")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "끼니 AI 추천 조회",
            description = "해당 끼니 추천이 없으면 Gemini를 1회 호출해 후보를 생성·저장하고, 있으면 캐시를 반환한다. "
                    + "meal 생략 시 현재 시각 기준 끼니.")
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<TodayRecommendationResponse>> getToday(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "끼니(BREAKFAST/LUNCH/DINNER, 생략 시 현재 시각 기준)")
            @RequestParam(required = false) MealTime meal) {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.getToday(userId, meal)));
    }

    @Operation(summary = "끼니 AI 추천 새로고침",
            description = "미리 받아둔 다음 후보를 반환한다(Gemini 재호출 없음). 마지막 후보 다음은 첫 후보로 순환하며, 횟수 제한이 없다.")
    @PostMapping("/today/refresh")
    public ResponseEntity<ApiResponse<TodayRecommendationResponse>> refresh(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "끼니(BREAKFAST/LUNCH/DINNER, 생략 시 현재 시각 기준)")
            @RequestParam(required = false) MealTime meal) {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.refresh(userId, meal)));
    }
}
