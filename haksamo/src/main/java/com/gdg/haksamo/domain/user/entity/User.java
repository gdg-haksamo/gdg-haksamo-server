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

    // RESTAURANT_ADMIN이 관리하는 식당. USER/SUPER_ADMIN은 null. (Restaurant 엔티티 도입 후 FK로 연결 예정)
    @Column(name = "managed_restaurant_id")
    private Long managedRestaurantId;

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

    /** 최상위 관리자(운영팀) 생성 — 부트스트랩 시더 전용. password는 이미 인코딩된 값이어야 한다. */
    public static User createSuperAdmin(String email, String encodedPassword, String nickname) {
        User user = new User(email, encodedPassword, nickname, null, null);
        user.role = Role.SUPER_ADMIN;
        return user;
    }

    /** 식당 운영자 계정 생성 — SUPER_ADMIN이 발급. password는 이미 인코딩된 값이어야 한다. */
    public static User createRestaurantAdmin(String email, String encodedPassword, String nickname, Long restaurantId) {
        User user = new User(email, encodedPassword, nickname, null, null);
        user.role = Role.RESTAURANT_ADMIN;
        user.managedRestaurantId = restaurantId;
        return user;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /**
     * 권한·담당 식당 변경 (SUPER_ADMIN의 계정 관리 API 전용).
     * RESTAURANT_ADMIN이 아닌 권한으로 바꾸면 담당 식당은 자동으로 해제(null)된다.
     */
    public void changeRole(Role role, Long managedRestaurantId) {
        this.role = role;
        this.managedRestaurantId = (role == Role.RESTAURANT_ADMIN) ? managedRestaurantId : null;
    }

    /** 비밀번호 재설정 (SUPER_ADMIN의 계정 관리 API 전용). 이미 인코딩된 값이어야 한다. */
    public void resetPassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public boolean isSuperAdmin() {
        return this.role == Role.SUPER_ADMIN;
    }

    public boolean canManageRestaurant(Long restaurantId) {
        if (this.role == Role.SUPER_ADMIN) {
            return true; // 운영팀은 모든 식당 관리
        }
        return this.role == Role.RESTAURANT_ADMIN
                && this.managedRestaurantId != null
                && this.managedRestaurantId.equals(restaurantId);
    }
}