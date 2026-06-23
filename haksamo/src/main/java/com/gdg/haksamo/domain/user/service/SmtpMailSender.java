package com.gdg.haksamo.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * prod용 실제 SMTP 발송. spring.mail.* (host/port/username/password)를 env로 주입해야 한다.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;

    @Override
    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[학사모] 이메일 인증번호");
        message.setText("인증번호는 " + code + " 입니다. 제한 시간 내에 입력해주세요.");
        javaMailSender.send(message);
    }
}