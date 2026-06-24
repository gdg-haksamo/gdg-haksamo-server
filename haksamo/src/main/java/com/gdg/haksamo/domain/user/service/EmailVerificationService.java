package com.gdg.haksamo.domain.user.service;

import com.gdg.haksamo.domain.user.entity.EmailVerification;
import com.gdg.haksamo.domain.user.repository.EmailVerificationRepository;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 1단계 이메일 인증.
 * - send-code: 중복 이메일 차단 후 6자리 인증번호 발송(재발송 시 기존 코드 무효화)
 * - verify-code: 만료/시도횟수/일치 검사 후 인증 완료 표시
 * - assertVerified: 회원가입 시점에 "유효한 인증 완료" 재검증
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final MailSender mailSender;

    @Value("${app.email-verification.code-ttl}")
    private long codeTtl;
    @Value("${app.email-verification.verified-ttl}")
    private long verifiedTtl;
    @Value("${app.email-verification.max-attempts}")
    private int maxAttempts;

    @Transactional
    public void sendCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(codeTtl * 1_000_000);

        emailVerificationRepository.findByEmail(email)
                .ifPresentOrElse(
                        v -> v.reissue(code, expiresAt),
                        () -> emailVerificationRepository.save(EmailVerification.issue(email, code, expiresAt)));

        mailSender.sendVerificationCode(email, code);
    }

    // noRollbackFor: 인증번호 불일치로 예외를 던져도 attemptCount 증가는 커밋되어야 시도횟수 제한이 동작함
    @Transactional(noRollbackFor = BusinessException.class)
    public void verifyCode(String email, String inputCode) {
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        if (verification.isExpired(now)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (verification.getAttemptCount() >= maxAttempts) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOO_MANY_ATTEMPTS);
        }
        if (!verification.matches(inputCode)) {
            verification.increaseAttempt();
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }
        verification.markVerified(now.plusNanos(verifiedTtl * 1_000_000));
    }

    /** 회원가입 직전 호출: 유효한 인증 완료가 아니면 가입 차단. */
    @Transactional(readOnly = true)
    public void assertVerified(String email) {
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED));
        if (!verification.isVerifiedValid(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    /** 가입 완료 후 재사용 방지를 위해 인증 내역 제거. */
    @Transactional
    public void consume(String email) {
        emailVerificationRepository.findByEmail(email)
                .ifPresent(emailVerificationRepository::delete);
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
