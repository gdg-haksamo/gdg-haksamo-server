package com.gdg.haksamo.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdg.haksamo.global.exception.ErrorCode;
import com.gdg.haksamo.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 인증은 됐지만 권한이 없는 사용자(예: USER가 ADMIN API 접근)에게 403을 공통 ApiResponse로 응답.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
}