package com.gdg.haksamo.domain.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 설정 수정 요청 (보낸 필드만 변경, null이면 그대로 유지)")
public record NotificationSettingsUpdateRequest(

        @Schema(description = "마스터 푸시 on/off. false면 하위 4개 전부 OFF. true인데 하위가 전부 OFF면 하위 4개 전부 ON")
        Boolean pushNotificationEnabled,

        @Schema(description = "아침 알림")
        Boolean breakfast,

        @Schema(description = "점심 알림")
        Boolean lunch,

        @Schema(description = "저녁 알림")
        Boolean dinner,

        @Schema(description = "이벤트/공지 알림")
        Boolean event
) {
}
