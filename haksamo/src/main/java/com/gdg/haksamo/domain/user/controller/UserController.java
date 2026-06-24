package com.gdg.haksamo.domain.user.controller;

import com.gdg.haksamo.domain.user.dto.FcmTokenRequest;
import com.gdg.haksamo.domain.user.service.UserService;
import com.gdg.haksamo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User - 사용자", description = "로그인 사용자의 마이페이지/설정")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 로그인 사용자의 FCM 토큰 등록/갱신. principal = userId(Long). */
    @Operation(summary = "FCM 토큰 등록/갱신", description = "푸시 알림용 FCM 토큰을 저장한다.")
    @PatchMapping("/me/fcm-token")
    public ResponseEntity<ApiResponse<Void>> registerFcmToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FcmTokenRequest request) {
        userService.registerFcmToken(userId, request.fcmToken());
        return ResponseEntity.ok(ApiResponse.success());
    }
}