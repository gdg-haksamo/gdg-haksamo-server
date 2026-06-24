package com.gdg.haksamo.domain.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 설정")
public record NotificationSettingsResponse(

        @Schema(description = "마스터 푸시 on/off (하위 4개 중 하나라도 켜져 있으면 자동 ON)")
        boolean pushNotificationEnabled,

        @Schema(description = "아침 알림")
        boolean breakfast,

        @Schema(description = "점심 알림")
        boolean lunch,

        @Schema(description = "저녁 알림")
        boolean dinner,

        @Schema(description = "이벤트/공지 알림")
        boolean event
) {
}
