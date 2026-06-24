package com.gdg.haksamo.domain.mypage.controller;

import com.gdg.haksamo.domain.mypage.dto.FavoriteRestaurantsUpdateRequest;
import com.gdg.haksamo.domain.mypage.dto.MyPageResponse;
import com.gdg.haksamo.domain.mypage.dto.NotificationSettingsResponse;
import com.gdg.haksamo.domain.mypage.dto.NotificationSettingsUpdateRequest;
import com.gdg.haksamo.domain.mypage.dto.PreferencesUpdateRequest;
import com.gdg.haksamo.domain.mypage.service.MyPageService;
import com.gdg.haksamo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "마이페이지")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me")
public class MyPageController {

    private final MyPageService myPageService;

    @Operation(summary = "마이페이지 조회", description = "프로필, 통계, 자주 가는 식당, 선호 키워드, 알림 설정을 한 번에 조회합니다.")
    @GetMapping
    public ApiResponse<MyPageResponse> getMyPage(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(myPageService.getMyPage(userId));
    }

    @Operation(summary = "자주 가는 식당 수정", description = "선호 식당 ID 1개를 설정합니다. null이면 선호 식당을 해제합니다.")
    @PatchMapping("/favorite-restaurants")
    public ApiResponse<Void> updateFavoriteRestaurants(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FavoriteRestaurantsUpdateRequest request
    ) {
        myPageService.updateFavoriteRestaurants(userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "선호 키워드 수정", description = "보낸 키워드 목록으로 전체 교체합니다.")
    @PatchMapping("/preferences")
    public ApiResponse<Void> updatePreferences(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PreferencesUpdateRequest request
    ) {
        myPageService.updatePreferences(userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "알림 설정 수정", description = "보낸 필드만 변경합니다. 마스터 토글과 하위 4개 토글은 서로 연동됩니다.")
    @PatchMapping("/notification-settings")
    public ApiResponse<NotificationSettingsResponse> updateNotificationSettings(
            @AuthenticationPrincipal Long userId,
            @RequestBody NotificationSettingsUpdateRequest request
    ) {
        return ApiResponse.success(myPageService.updateNotificationSettings(userId, request));
    }
}
