package com.gdg.haksamo.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
        @NotBlank(message = "FCM 토큰을 입력해주세요.")
        String fcmToken
) {
}