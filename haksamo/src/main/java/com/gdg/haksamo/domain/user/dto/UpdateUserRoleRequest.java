package com.gdg.haksamo.domain.user.dto;

import com.gdg.haksamo.domain.user.entity.Role;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 권한 변경 요청 — SUPER_ADMIN 전용.
 * role=RESTAURANT_ADMIN 인 경우 managedRestaurantId 필수.
 * 그 외 권한(USER/SUPER_ADMIN)으로 바꾸면 담당 식당은 자동 해제된다.
 */
public record UpdateUserRoleRequest(
        @NotNull(message = "변경할 권한(role)을 지정해주세요.")
        Role role,

        Long managedRestaurantId
) {
}