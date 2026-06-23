package com.gdg.haksamo.domain.user.entity;

import com.gdg.haksamo.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원. 이메일 유니크. 알림 설정 5종은 가입 시 기본 ON.
 * (테이블명은 MySQL 예약어라 백틱으로 quote → ERD의 `User`와 매칭)
 */
@Entity
@Getter
@Table(name = "`User`", uniqueConstraints = @UniqueConstraint(name = "uq_user_email", columnNames = "email"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    private String department; // 학과 (회원가입 시 입력)
    private Integer grade;      // 학년

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String fcmToken;

    @Column(nullable = false)
    private boolean pushNotificationEnabled;
    @Column(nullable = false)
    private boolean notificationBreakfastEnabled;
    @Column(nullable = false)
    private boolean notificationLunchEnabled;
    @Column(nullable = false)
    private boolean notificationDinnerEnabled;
    @Column(nullable = false)
    private boolean notificationEventEnabled;

    @Builder
    private User(String email, String password, String nickname, String department, Integer grade) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.department = department;
        this.grade = grade;
        this.role = Role.USER;
        this.pushNotificationEnabled = true;
        this.notificationBreakfastEnabled = true;
        this.notificationLunchEnabled = true;
        this.notificationDinnerEnabled = true;
        this.notificationEventEnabled = true;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}