package com.gdg.haksamo.domain.menu.controller;

import com.gdg.haksamo.domain.menu.dto.ManagedMenuResponse;
import com.gdg.haksamo.domain.menu.dto.MenuAdminResponse;
import com.gdg.haksamo.domain.menu.dto.MenuCreateRequest;
import com.gdg.haksamo.domain.menu.dto.MenuUpdateRequest;
import com.gdg.haksamo.domain.menu.service.MenuAdminService;
import com.gdg.haksamo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 식당 관리자 메뉴 관리 API.
 * 조회는 관리자면 어느 식당이든 가능하고, 쓰기는 서비스의 RestaurantAdminGuard가 본인 담당 식당만 허용한다.
 */
@Tag(name = "메뉴 관리(관리자)")
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuAdminController {

    private final MenuAdminService menuAdminService;

    @Operation(
            summary = "관리자 메뉴 조회",
            description = "관리자 화면용 메뉴 목록(scheduleId·품절여부 포함). restaurantId 생략 시 본인 담당 식당, "
                    + "지정 시 해당 식당(SUPER_ADMIN/타 식당 열람). date 생략 시 오늘."
    )
    @GetMapping("/manage")
    public ApiResponse<List<ManagedMenuResponse>> getManagedMenus(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회 식당 id (생략 시 본인 담당 식당)", example = "1")
            @RequestParam(required = false) Long restaurantId,
            @Parameter(description = "조회 날짜 (yyyy-MM-dd, 생략 시 오늘)", example = "2026-06-24")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(menuAdminService.getManagedMenus(userId, restaurantId, date));
    }

    @Operation(summary = "품절 토글", description = "편성(scheduleId)의 품절 여부를 토글합니다. 본인 담당 식당만 가능(403 A008).")
    @PatchMapping("/schedules/{scheduleId}/sold-out")
    public ApiResponse<Void> toggleSoldOut(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long scheduleId
    ) {
        menuAdminService.toggleSoldOut(userId, scheduleId);
        return ApiResponse.success();
    }

    @Operation(summary = "신메뉴 등록", description = "메뉴(카탈로그) + 당일 편성을 생성합니다. 본인 담당 식당만 가능(403 A008).")
    @PostMapping
    public ApiResponse<MenuAdminResponse> createMenu(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MenuCreateRequest request
    ) {
        return ApiResponse.success(menuAdminService.createMenu(userId, request));
    }

    @Operation(summary = "메뉴 이름·가격 수정", description = "보낸 필드만 변경합니다. 본인 담당 식당만 가능(403 A008).")
    @PatchMapping("/{menuId}")
    public ApiResponse<MenuAdminResponse> updateMenu(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long menuId,
            @Valid @RequestBody MenuUpdateRequest request
    ) {
        return ApiResponse.success(menuAdminService.updateMenu(userId, menuId, request));
    }

    @Operation(summary = "메뉴 삭제", description = "메뉴와 연결된 리뷰·도움됐어요·편성을 함께 삭제합니다. 본인 담당 식당만 가능(403 A008).")
    @DeleteMapping("/{menuId}")
    public ApiResponse<Void> deleteMenu(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long menuId
    ) {
        menuAdminService.deleteMenu(userId, menuId);
        return ApiResponse.success();
    }
}
