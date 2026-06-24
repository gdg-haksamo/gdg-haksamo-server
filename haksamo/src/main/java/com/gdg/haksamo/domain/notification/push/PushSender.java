package com.gdg.haksamo.domain.notification.push;

import java.util.Map;

/**
 * FCM 푸시 발송 추상화. 자격증명 유무로 구현이 갈린다.
 * <ul>
 *   <li>자격증명 미설정(기본) : {@link LoggingPushSender} — 실제 발송 없이 로그만(앱 항상 기동)</li>
 *   <li>자격증명 설정          : {@link FcmPushSender} — Firebase Admin SDK로 실제 발송</li>
 * </ul>
 * (prod 전용 {@code @Profile} 대신 {@code notification.push.firebase.enabled}로 분기 →
 *  자격증명 누락이 부팅을 막지 않게 한다. 이메일 SMTP 미설정 크래시(과거 PR #40)의 재발 방지)
 */
public interface PushSender {

    /**
     * 한 사용자에게 푸시 1건 발송.
     *
     * @return 실제 FCM로 전송됐으면 true. (로그 스텁이거나 전송 실패 시 false)
     */
    boolean send(String fcmToken, String title, String body, Map<String, String> data);
}
