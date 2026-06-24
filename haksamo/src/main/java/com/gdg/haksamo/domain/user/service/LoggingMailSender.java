package com.gdg.haksamo.domain.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * dev/로컬용. 실제 메일을 보내지 않고 인증번호를 로그로 출력한다.
 * → SMTP 계정 없이도 회원가입 흐름 전체를 테스트할 수 있다.
 */
@Slf4j
@Component
@Profile("!prod")
public class LoggingMailSender implements MailSender {

    @Override
    public void sendVerificationCode(String email, String code) {
        log.info("[DEV-MAIL] 이메일 인증번호 → {} : {} (실제 발송 안 함)", email, code);
    }
}