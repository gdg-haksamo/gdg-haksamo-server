package com.gdg.haksamo.domain.notification.controller;

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
        return ApiResponse.success(notificationService.pushTodayRecommendation(request.userId()));
    }
}
