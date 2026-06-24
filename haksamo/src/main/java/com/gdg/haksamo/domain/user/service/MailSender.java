package com.gdg.haksamo.domain.user.service;

/**
 * 인증번호 발송 추상화. 프로파일별 구현(dev=로그, prod=SMTP)을 주입한다.
 */
public interface MailSender {

    void sendVerificationCode(String email, String code);
}