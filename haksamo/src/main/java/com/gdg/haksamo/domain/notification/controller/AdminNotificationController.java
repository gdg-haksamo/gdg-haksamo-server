package com.gdg.haksamo.domain.notification.controller;

import com.gdg.haksamo.domain.notification.dto.DemoPushRequest;
import com.gdg.haksamo.domain.notification.dto.DemoPushResponse;
import com.gdg.haksamo.domain.notification.dto.RecommendationPushRequest;
import com.gdg.haksamo.domain.notification.dto.RecommendationPushResponse;
import com.gdg.haksamo.domain.notification.service.NotificationService;
import com.gdg.haksamo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 관리(관리자) API. {@code /api/admin/**} 경로는 SecurityConfig에서 SUPER_ADMIN으로 게이트된다.
 */
@Tag(name = "Notification 관리(관리자)", description = "운영팀(SUPER_ADMIN)의 알림 발송")
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "대상 사용자에게 오늘 추천 푸시",
            description = "대상 사용자의 오늘 추천을 생성(또는 캐시)한 뒤 즉시 FCM 푸시한다. "
                    + "응답에 발송한 추천 내용이 포함되어 발송 전/후 확인이 가능하다. SUPER_ADMIN 전용.")
    @PostMapping("/recommendation-push")
    public ApiResponse<RecommendationPushResponse> pushRecommendation(
            @Valid @RequestBody RecommendationPushRequest request) {
        return ApiResponse.success(notificationService.pushTodayRecommendation(request.userId(), request.meal()));
    }

    @Operation(summary = "[데모] 입력 기반 추천 생성 + 푸시",
            description = "날짜·끼니·선호 키워드·선호 식당(1개)을 직접 입력해 추천 4개를 생성하고 1순위를 즉시 푸시한다. "
                    + "유저의 저장된 선호와 무관하게 시연 시나리오를 구성한다(추천 저장 안 함). SUPER_ADMIN 전용.")
    @PostMapping("/recommendation-push/demo")
    public ApiResponse<DemoPushResponse> demoPush(@Valid @RequestBody DemoPushRequest request) {
        return ApiResponse.success(notificationService.demoPush(request));
    }
}
