package com.gdg.haksamo.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 1단계(기본 정보) 요청.
 * 이메일 도메인 제한 없음 — 형식(@Email)과 중복만 검증.
 * 자주 가는 식당/선호 키워드(2·3단계)는 Restaurant/Preference 도메인 완성 후 연동 예정.
 */
public record SignUpRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 8, message = "닉네임은 2~8자여야 합니다.")
        String nickname,

        String department,
        Integer grade
) {
}