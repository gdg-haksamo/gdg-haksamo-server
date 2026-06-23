package com.gdg.haksamo.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 식당 운영자(RESTAURANT_ADMIN) 계정 발급 요청 — SUPER_ADMIN 전용.
 * 일반 회원가입과 달리 이메일 인증 단계가 없다(운영팀이 직접 발급).
 * restaurantId는 이 계정이 관리할 식당. 발급 후 운영자는 이 식당만 관리할 수 있다.
 */
public record CreateRestaurantAdminRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "닉네임(식당명 등)을 입력해주세요.")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        String nickname,

        @NotNull(message = "담당 식당(restaurantId)을 지정해주세요.")
        Long restaurantId
) {
}