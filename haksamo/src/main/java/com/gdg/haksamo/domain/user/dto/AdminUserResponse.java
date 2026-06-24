package com.gdg.haksamo.domain.user.dto;

import com.gdg.haksamo.domain.user.entity.Role;
import com.gdg.haksamo.domain.user.entity.User;

/**
 * 관리자(SUPER_ADMIN) 계정 관리 화면용 사용자 요약.
 * managedRestaurantId는 RESTAURANT_ADMIN일 때만 채워진다.
 */
public record AdminUserResponse(
        Long userId,
        String email,
        String nickname,
        Role role,
        Long managedRestaurantId
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getManagedRestaurantId());
    }
}