package com.gdg.haksamo.domain.user.dto;

/**
 * 클라이언트(FE)에 내려주는 Access Token. FE는 메모리에 보관해 Authorization 헤더로 사용.
 * Refresh Token은 본문에 노출하지 않고 httpOnly 쿠키로 전달한다.
 */
public record TokenResponse(
        String accessToken,
        long accessTokenExpiresIn // 밀리초
) {
}