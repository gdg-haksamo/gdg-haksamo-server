package com.gdg.haksamo.domain.user.dto;

/**
 * 서비스 → 컨트롤러 내부 전달용. accessToken은 응답 본문, refreshToken은 쿠키로 분리된다.
 */
public record TokenPair(
        String accessToken,
        String refreshToken
) {
}