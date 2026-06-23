package com.gdg.haksamo.domain.user;

import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부트스트랩 최상위 관리자(SUPER_ADMIN) 시더.
 * - ADMIN_EMAIL / ADMIN_PASSWORD (env)가 있으면, 해당 계정이 없을 때만 role=SUPER_ADMIN 계정을 1회 생성한다(멱등).
 * - 평문 비밀번호는 env/Secrets에만 존재하고 DB엔 BCrypt 해시로만 저장된다.
 * - DB는 유지(named volume)되므로 한 번 생성되면 이후 부팅에선 스킵된다.
 * - 비밀번호 변경/추가 관리자는 별도 처리(SQL 승격 또는 관리자 API).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;
    @Value("${app.admin.password:}")
    private String adminPassword;
    @Value("${app.admin.nickname:관리자}")
    private String adminNickname;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("관리자 시더 스킵: ADMIN_EMAIL/ADMIN_PASSWORD 미설정");
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("관리자 계정 이미 존재 → 스킵: {}", adminEmail);
            return;
        }
        // ADMIN_NICKNAME이 비어있는(빈 문자열) 채로 주입되면 @Value 기본값이 적용되지 않으므로 여기서 보정
        String nickname = adminNickname.isBlank() ? "관리자" : adminNickname;
        userRepository.save(User.createSuperAdmin(adminEmail, passwordEncoder.encode(adminPassword), nickname));
        log.info("최상위 관리자(SUPER_ADMIN) 계정 생성 완료: {}", adminEmail);
    }
}