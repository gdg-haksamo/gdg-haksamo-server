package com.gdg.haksamo.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * {@link com.gdg.haksamo.global.common.BaseTimeEntity}의 created_at/updated_at 자동 기록 활성화.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}