package com.gdg.haksamo.domain.user.service;

import com.gdg.haksamo.domain.preference.PreferenceService;
import com.gdg.haksamo.domain.restaurant.FavoriteRestaurantService;
import com.gdg.haksamo.domain.user.dto.LoginRequest;
import com.gdg.haksamo.domain.user.dto.SignUpRequest;
import com.gdg.haksamo.domain.user.dto.SignUpResponse;
import com.gdg.haksamo.domain.user.dto.TokenPair;
import com.gdg.haksamo.domain.user.entity.RefreshToken;
import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.RefreshTokenRepository;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import com.gdg.haksamo.global.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 / 로그인 / 토큰 재발급(회전) / 로그아웃.
 * Refresh Token은 해시로 저장해 DB 유출 시에도 원문이 노출되지 않게 한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;
    private final PreferenceService preferenceService;
    private final FavoriteRestaurantService favoriteRestaurantService;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        // 회원가입 1단계(이메일 인증)를 통과한 이메일만 가입 가능
        emailVerificationService.assertVerified(request.email());
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        User user = userRepository.save(User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .department(request.department())
                .build());
        // 가입 2·3단계 선택 정보 저장. 같은 트랜잭션이라 유저·식당·키워드가 원자적으로 커밋된다.
        favoriteRestaurantService.setFavorite(user, request.restaurantId());
        preferenceService.replaceKeywords(user, request.keywords());
        emailVerificationService.consume(request.email()); // 인증 내역 제거(재사용 방지)
        return new SignUpResponse(user.getId(), user.getEmail(), user.getNickname());
    }

    @Transactional
    public TokenPair login(LoginRequest request) {
        // 존재하지 않는 이메일/틀린 비밀번호 모두 동일 메시지로 통일(계정 존재 여부 노출 방지)
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issueTokens(user.getId(), user.getRole().name());
    }

    // noRollbackFor: 재사용 감지 시 토큰 삭제(revoke)가 예외 던져도 커밋되어야 실제로 무효화됨
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenPair reissue(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (!jwtTokenProvider.validate(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_MISMATCH));

        // 저장된 해시와 불일치 = 이미 회전된(또는 탈취된) 토큰 재사용 → 안전하게 전체 무효화
        if (!stored.getTokenHash().equals(hash(refreshToken))) {
            refreshTokenRepository.delete(stored);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return issueTokens(userId, user.getRole().name());
    }

    /**
     * 로그아웃 — 서버측 Refresh Token 폐기.
     * principal(userId)이 있으면 그걸로, 없으면(Access 만료/미첨부) 유효한 Refresh 쿠키에서 userId를 복원해 폐기한다.
     */
    @Transactional
    public void logout(Long userId, String refreshToken) {
        Long targetId = userId;
        if (targetId == null && refreshToken != null && !refreshToken.isBlank()
                && jwtTokenProvider.validate(refreshToken)) {
            targetId = jwtTokenProvider.getUserId(refreshToken);
        }
        if (targetId != null) {
            refreshTokenRepository.deleteByUserId(targetId);
        }
    }

    /** Access/Refresh 발급 + Refresh 해시를 사용자당 1행으로 upsert(회전). */
    private TokenPair issueTokens(Long userId, String role) {
        String accessToken = jwtTokenProvider.createAccessToken(userId, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);
        String refreshHash = hash(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                Instant.now().plusMillis(jwtTokenProvider.getRefreshExpiration()), ZoneId.systemDefault());

        refreshTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        existing -> existing.rotate(refreshHash, expiresAt),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .userId(userId)
                                .tokenHash(refreshHash)
                                .expiresAt(expiresAt)
                                .build()));
        return new TokenPair(accessToken, refreshToken);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
