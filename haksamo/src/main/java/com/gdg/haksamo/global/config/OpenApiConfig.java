package com.gdg.haksamo.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI(/swagger-ui.html) 문서 설정 + JWT Bearer 인증 스킴.
 * Swagger UI 우상단 Authorize에 Access Token을 넣으면 인증 필요한 API도 테스트할 수 있다.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI haksamoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("학사모 API")
                        .version("v1")
                        .description("경북대 학식 추천 PWA 백엔드 API 문서"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}