package com.gdg.haksamo.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 요청 — SUPER_ADMIN 전용(운영자 분실 대응 등).
 */
public record ResetPasswordRequest(
        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다.")  // BCrypt 72바이트 한계
        String newPassword
) {
}