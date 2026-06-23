package com.gdg.haksamo.domain.user.controller;

import com.gdg.haksamo.domain.user.dto.LoginRequest;
import com.gdg.haksamo.domain.user.dto.SendCodeRequest;
import com.gdg.haksamo.domain.user.dto.SignUpRequest;
import com.gdg.haksamo.domain.user.dto.SignUpResponse;
import com.gdg.haksamo.domain.user.dto.TokenPair;
import com.gdg.haksamo.domain.user.dto.TokenResponse;
import com.gdg.haksamo.domain.user.dto.VerifyCodeRequest;
import com.gdg.haksamo.domain.user.service.AuthService;
import com.gdg.haksamo.domain.user.service.EmailVerificationService;
import com.gdg.haksamo.global.response.ApiResponse;
import com.gdg.haksamo.global.security.CookieUtil;
import com.gdg.haksamo.global.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API. Access Token은 본문, Refresh Token은 httpOnly 쿠키로 내려준다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final CookieUtil cookieUtil;
    private final JwtTokenProvider jwtTokenProvider;

    // === 회원가입 1단계: 이메일 인증 ===

    @PostMapping("/email/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(@Valid @RequestBody SendCodeRequest request) {
        emailVerificationService.sendCode(request.email());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/email/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ResponseEntity.ok(ApiResponse.success());
    }

    // === 회원가입 2단계 이후: 계정 생성 (이메일 인증 완료 필수) ===

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenPair pair = authService.login(request);
        return tokenResponse(pair);
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(HttpServletRequest request) {
        String refreshToken = cookieUtil.resolve(request);
        TokenPair pair = authService.reissue(refreshToken);
        return tokenResponse(pair);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Long userId) {
        if (userId != null) {
            authService.logout(userId);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clear().toString())
                .body(ApiResponse.success());
    }

    /** Access Token은 본문, Refresh Token은 Set-Cookie로 동시에 내려주는 공통 응답. */
    private ResponseEntity<ApiResponse<TokenResponse>> tokenResponse(TokenPair pair) {
        String cookie = cookieUtil.create(pair.refreshToken(), jwtTokenProvider.getRefreshExpiration()).toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie)
                .body(ApiResponse.success(new TokenResponse(pair.accessToken(), jwtTokenProvider.getAccessExpiration())));
    }
}