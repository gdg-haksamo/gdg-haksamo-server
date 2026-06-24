package com.gdg.haksamo.domain.user.controller;

import com.gdg.haksamo.domain.user.dto.AdminUserListResponse;
import com.gdg.haksamo.domain.user.dto.AdminUserResponse;
import com.gdg.haksamo.domain.user.dto.CreateRestaurantAdminRequest;
import com.gdg.haksamo.domain.user.dto.ResetPasswordRequest;
import com.gdg.haksamo.domain.user.dto.UpdateUserRoleRequest;
import com.gdg.haksamo.domain.user.entity.Role;
import com.gdg.haksamo.domain.user.service.AdminUserService;
import com.gdg.haksamo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 최상위 관리자(SUPER_ADMIN) 전용 계정 관리 API.
 * 경로 전체가 SecurityConfig에서 hasRole('SUPER_ADMIN')로 보호된다(/api/admin/**).
 * principal = userId(Long) — 본인 계정 잠금 방지 검사에 사용.
 */
@Tag(name = "Admin - 계정 관리", description = "운영팀(SUPER_ADMIN)이 식당 운영자 계정과 사용자 권한을 관리한다.")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "계정 목록 조회", description = "role 파라미터로 권한별 필터링(미지정 시 전체).")
    @GetMapping
    public ApiResponse<AdminUserListResponse> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(adminUserService.getUsers(role, pageable));
    }

    @Operation(summary = "식당 운영자 계정 발급", description = "RESTAURANT_ADMIN 계정을 만들고 담당 식당을 지정한다.")
    @PostMapping("/restaurant-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminUserResponse> createRestaurantAdmin(
            @Valid @RequestBody CreateRestaurantAdminRequest request) {
        return ApiResponse.success(adminUserService.createRestaurantAdmin(request));
    }

    @Operation(summary = "권한/담당 식당 변경", description = "사용자의 role과 담당 식당을 변경한다(본인 계정 제외).")
    @PatchMapping("/{userId}/role")
    public ApiResponse<AdminUserResponse> updateRole(
            @AuthenticationPrincipal Long actorUserId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return ApiResponse.success(adminUserService.updateRole(actorUserId, userId, request));
    }

    @Operation(summary = "비밀번호 재설정", description = "운영자 분실 대응 등으로 비밀번호를 강제 재설정한다.")
    @PatchMapping("/{userId}/password")
    public ApiResponse<Void> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody ResetPasswordRequest request) {
        adminUserService.resetPassword(userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "계정 삭제", description = "사용자 계정을 삭제한다(본인 계정 제외).")
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(
            @AuthenticationPrincipal Long actorUserId,
            @PathVariable Long userId) {
        adminUserService.deleteUser(actorUserId, userId);
        return ApiResponse.success();
    }
}