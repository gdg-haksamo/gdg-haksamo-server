package com.gdg.haksamo.domain.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Firebase 초기화 — {@code notification.push.firebase.enabled=true}일 때만 구성된다.
 *
 * <p>자격증명(서비스계정 JSON)은 {@code FCM_CREDENTIALS_JSON} env로 JSON 문자열 그대로 주입한다
 * (컨테이너/Secrets 친화). enabled=false(기본)면 이 설정 자체가 로드되지 않아
 * 자격증명이 없어도 앱이 정상 기동한다.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "notification.push.firebase", name = "enabled", havingValue = "true")
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp(
            @Value("${notification.push.firebase.credentials-json}") String credentialsJson)
            throws IOException {
        if (credentialsJson == null || credentialsJson.isBlank()) {
            throw new IllegalStateException(
                    "notification.push.firebase.enabled=true 이지만 FCM_CREDENTIALS_JSON(서비스계정 JSON)이 비어 있습니다.");
        }
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)));
        FirebaseApp app = FirebaseApp.initializeApp(
                FirebaseOptions.builder().setCredentials(credentials).build());
        log.info("FirebaseApp 초기화 완료 (FCM 실제 발송 활성)");
        return app;
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
