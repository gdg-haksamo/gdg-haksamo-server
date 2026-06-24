package com.gdg.haksamo.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * dev 프로파일 전용. 인증이 비어 있으면 항상 userId=1을 주입한다.
 * → Auth 완성 전 다른 도메인의 인증 필요 API를 로그인 없이 개발/테스트하기 위한 장치.
 *
 * 주의: 반드시 dev 프로파일에서만 등록되어야 한다 (SecurityConfig 참고).
 * prod에서 활성화되면 모든 요청이 인증된 것으로 처리되는 인증 우회 취약점이 된다.
 * 실제 Bearer 토큰이 오면 JwtAuthenticationFilter가 먼저 인증을 채우므로 이 필터는 건너뛴다.
 */
public class DummyAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            var authentication = new UsernamePasswordAuthenticationToken(1L, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}