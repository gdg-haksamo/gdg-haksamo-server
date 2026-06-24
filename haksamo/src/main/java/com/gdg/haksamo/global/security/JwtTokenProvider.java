package com.gdg.haksamo.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 발급/검증. Access Token(role 포함)과 Refresh Token을 생성한다.
 * 시크릿/만료값은 application.yml의 jwt.* 프로퍼티에서 주입.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String createAccessToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpiration))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // jti: 매 발급마다 고유 → 회전 시 토큰이 항상 달라져 재사용 감지가 동작
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiration))
                .signWith(key)
                .compact();
    }

    /** 서명/만료 검증. 필터에서 조용히 인증 스킵하기 위해 예외 대신 boolean 반환. */
    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public String getRole(String token) {
        return parse(token).get(ROLE_CLAIM, String.class);
    }

    public long getAccessExpiration() {
        return accessExpiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}