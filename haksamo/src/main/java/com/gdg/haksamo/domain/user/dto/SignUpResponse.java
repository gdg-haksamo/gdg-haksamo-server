package com.gdg.haksamo.domain.user.dto;

public record SignUpResponse(
        Long userId,
        String email,
        String nickname
) {
}