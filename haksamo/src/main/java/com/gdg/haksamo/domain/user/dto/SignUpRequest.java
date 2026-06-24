package com.gdg.haksamo.domain.user.dto;

import com.gdg.haksamo.domain.preference.PreferenceKeyword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 회원가입 요청.
 * 이메일 도메인 제한 없음 — 형식(@Email)과 중복만 검증.
 * 학년(grade)은 회원가입에서 입력받지 않기로 결정 → 필드 제거.
 * 선호 키워드(가입 3단계)는 선택 입력 — null/빈 목록이면 저장하지 않는다.
 * 자주 가는 식당(2단계)은 Restaurant 도메인 연동 후 추가 예정.
 */
public record SignUpRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        // 상한 72자: BCrypt는 72바이트 이후를 무시(절단)하므로 그 안에서 받는다.
        @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다.")
        String password,

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 8, message = "닉네임은 2~8자여야 합니다.")
        String nickname,

        String department,

        // 가입 3단계 선호 키워드(선택). 미입력 시 null/빈 목록 허용.
        List<PreferenceKeyword> keywords
) {
}