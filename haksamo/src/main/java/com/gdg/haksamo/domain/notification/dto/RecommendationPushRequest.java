package com.gdg.haksamo.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 관리자 추천 푸시 요청 — 대상 사용자.
 */
public record RecommendationPushRequest(

        @Schema(description = "푸시를 받을 대상 사용자 id", example = "1")
        @NotNull(message = "대상 사용자 id를 입력해주세요.")
        Long userId
) {
}
