package com.gdg.haksamo.domain.notification.entity;

import com.gdg.haksamo.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발송한 알림 이력. (FCM 성공 여부와 무관하게 "보낸 기록"을 남긴다 — erd-decisions #9)
 * MVP에선 알림 내역 조회 UI는 후순위지만 적재는 해 둔다.
 */
@Entity
@Getter
@Table(name = "Notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean isRead;

    private Notification(Long userId, String title, String content) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.isRead = false;
    }

    public static Notification create(Long userId, String title, String content) {
        return new Notification(userId, title, content);
    }
}
