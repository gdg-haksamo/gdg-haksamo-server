package com.gdg.haksamo.domain.user.entity;

import com.gdg.haksamo.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 1단계 이메일 인증 상태. 이메일당 1행(재요청 시 덮어씀).
 * 도메인 제한은 없음 — 이메일 "소유"를 검증하는 것이지 도메인을 제한하는 게 아니다.
 */
@Entity
@Getter
@Table(name = "email_verification",
        uniqueConstraints = @UniqueConstraint(name = "uq_email_verification_email", columnNames = "email"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // 인증번호 유효 만료

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "verified_expires_at")
    private LocalDateTime verifiedExpiresAt; // 인증 완료 상태의 만료(회원가입 마감 시한)

    private EmailVerification(String email, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
        this.verified = false;
        this.attemptCount = 0;
    }

    public static EmailVerification issue(String email, String code, LocalDateTime expiresAt) {
        return new EmailVerification(email, code, expiresAt);
    }

    /** 인증번호 재발송: 코드/만료 갱신 + 상태 초기화. */
    public void reissue(String code, LocalDateTime expiresAt) {
        this.code = code;
        this.expiresAt = expiresAt;
        this.verified = false;
        this.attemptCount = 0;
        this.verifiedExpiresAt = null;
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public void increaseAttempt() {
        this.attemptCount++;
    }

    public boolean matches(String input) {
        return this.code.equals(input);
    }

    public void markVerified(LocalDateTime verifiedExpiresAt) {
        this.verified = true;
        this.verifiedExpiresAt = verifiedExpiresAt;
    }

    /** 회원가입 시점에 "유효한 인증 완료" 상태인지. */
    public boolean isVerifiedValid(LocalDateTime now) {
        return verified && verifiedExpiresAt != null && now.isBefore(verifiedExpiresAt);
    }
}