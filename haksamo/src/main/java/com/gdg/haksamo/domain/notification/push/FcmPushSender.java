package com.gdg.haksamo.domain.notification.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 자격증명 설정 시(prod 등)용. Firebase Admin SDK로 실제 FCM 발송.
 * {@code notification.push.firebase.enabled=true} + 서비스계정 JSON이 있을 때만 빈으로 등록된다
 * (그때만 {@link com.gdg.haksamo.domain.notification.config.FirebaseConfig}가 FirebaseMessaging을 제공).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "notification.push.firebase", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class FcmPushSender implements PushSender {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public boolean send(String fcmToken, String title, String body, Map<String, String> data) {
        // com.google.firebase.messaging.Notification — 우리 엔티티 Notification과 이름이 겹쳐 FQN 사용
        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();
        try {
            String messageId = firebaseMessaging.send(message);
            log.info("FCM 발송 완료 messageId={}", messageId);
            return true;
        } catch (FirebaseMessagingException e) {
            // 토큰 만료/무효 등은 단건 실패로 처리(흐름은 계속). 운영에선 무효 토큰 정리 로직 후속.
            log.warn("FCM 발송 실패 errorCode={}, msg={}", e.getMessagingErrorCode(), e.getMessage());
            return false;
        }
    }
}
