package com.gdg.haksamo.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyCodeRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "인증번호를 입력해주세요.")
        @Pattern(regexp = "\\d{6}", message = "인증번호는 6자리 숫자여야 합니다.")  // 발송 코드와 동일 형식
        String code
) {
}