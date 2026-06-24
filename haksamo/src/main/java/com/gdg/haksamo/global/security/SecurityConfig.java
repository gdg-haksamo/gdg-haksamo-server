package com.gdg.haksamo.global.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 시큐리티 설정.
 * - 세션리스(JWT) + CSRF off + CORS(자격증명 허용)
 * - /api/auth/** 는 공개, 나머지는 인증 필요
 * - JwtAuthenticationFilter로 Bearer 토큰 인증
 * - dev 프로파일에서만 DummyAuthFilter를 뒤에 추가(토큰 없으면 userId=1 주입)
 */
@Configuration
@RequiredArgsConstructor
public class

SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;


    /** dev 프로파일에서만 빈 등록 → prod에는 존재하지 않아 인증 우회 불가. */
    @Bean
    @Profile("dev")
    public DummyAuthFilter dummyAuthFilter() {
        return new DummyAuthFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectProvider<DummyAuthFilter> dummyAuthFilterProvider)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // Swagger / OpenAPI 문서 (운영에서는 노출 제한 검토)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // 관리자 계정 관리 API는 운영팀(SUPER_ADMIN) 전용.
                        // (경로 기반 게이트 → 거부 시 ExceptionTranslationFilter→JwtAccessDeniedHandler로 403 ApiResponse)
                        // 식당 운영자(RESTAURANT_ADMIN)의 메뉴/리뷰 조작은 별도 경로가 아니라
                        // 각 도메인 서비스에서 RestaurantAdminGuard로 "담당 식당"만 허용한다.
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        // [비로그인 접근정책] 메인 "오늘의 학식 메뉴 리스트"만 공개.
                        // 메뉴 상세 / 리뷰 / 식당 목록 등은 공개하지 않음 → 비로그인 시 401(ApiResponse) → FE가 로그인 유도
                        // (dev 프로파일은 DummyAuthFilter가 토큰 없으면 userId=1로 채워줘서 로그인 없이도 테스트 가능)
                        .requestMatchers(HttpMethod.GET, "/api/menus").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        DummyAuthFilter dummyAuthFilter = dummyAuthFilterProvider.getIfAvailable();
        if (dummyAuthFilter != null) {
            http.addFilterAfter(dummyAuthFilter, JwtAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // Refresh Token 쿠키 전송을 위해 필수
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
