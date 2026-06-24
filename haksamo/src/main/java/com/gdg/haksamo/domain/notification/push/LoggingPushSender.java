package com.gdg.haksamo.domain.notification.push;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 자격증명 미설정(기본)용. 실제 발송 없이 로그만 남긴다.
 * → Firebase 설정 없이도 추천 생성→푸시 흐름 전체를 로컬에서 테스트할 수 있다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "notification.push.firebase", name = "enabled",
        havingValue = "false", matchIfMissing = true)
public class LoggingPushSender implements PushSender {

    @Override
    public boolean send(String fcmToken, String title, String body, Map<String, String> data) {
        log.info("[DEV-PUSH] FCM 발송(로그만) → token={}, title='{}', body='{}', data={}",
                maskToken(fcmToken), title, body, data);
        return false;
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 8) + "...";
    }
}
