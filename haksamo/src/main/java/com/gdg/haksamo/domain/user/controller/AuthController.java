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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth - 인증", description = "이메일 인증·회원가입·로그인·토큰 재발급·로그아웃")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final CookieUtil cookieUtil;
    private final JwtTokenProvider jwtTokenProvider;

    // === 회원가입 1단계: 이메일 인증 ===

    @Operation(summary = "이메일 인증번호 발송", description = "회원가입 1단계. 6자리 코드 발송(유효 3분). dev는 서버 로그로 출력.")
    @PostMapping("/email/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(@Valid @RequestBody SendCodeRequest request) {
        emailVerificationService.sendCode(request.email());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "이메일 인증번호 검증", description = "코드 일치 시 인증 완료(이후 30분 내 회원가입 가능). 시도 5회 제한.")
    @PostMapping("/email/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ResponseEntity.ok(ApiResponse.success());
    }

    // === 회원가입 2단계 이후: 계정 생성 (이메일 인증 완료 필수) ===

    @Operation(summary = "회원가입", description = "이메일 인증 완료 필수. 도메인 제한 없음. 비밀번호 8~72자, 닉네임 2~8자.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "로그인", description = "Access Token은 본문, Refresh Token은 httpOnly 쿠키로 발급.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenPair pair = authService.login(request);
        return tokenResponse(pair);
    }

    @Operation(summary = "Access Token 재발급", description = "Refresh 쿠키로 Access 재발급 + Refresh 회전. 재사용 감지 시 전체 무효화.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(HttpServletRequest request) {
        String refreshToken = cookieUtil.resolve(request);
        TokenPair pair = authService.reissue(refreshToken);
        return tokenResponse(pair);
    }

    @Operation(summary = "로그아웃", description = "서버측 Refresh Token 폐기 + 쿠키 만료. Access 없어도 쿠키로 폐기.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Long userId,
                                                    HttpServletRequest request) {
        // Access Token이 없거나 만료돼 principal이 null이어도, Refresh 쿠키로 서버측 토큰을 폐기한다.
        // (그렇지 않으면 쿠키만 지워지고 서버의 refresh_token이 살아남아 재사용 위험)
        authService.logout(userId, cookieUtil.resolve(request));
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