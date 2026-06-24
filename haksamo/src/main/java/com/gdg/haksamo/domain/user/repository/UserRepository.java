package com.gdg.haksamo.domain.user.repository;

import com.gdg.haksamo.domain.user.entity.Role;
import com.gdg.haksamo.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // 관리자 계정 관리: role 필터 조회 (예: RESTAURANT_ADMIN 목록)
    Page<User> findByRole(Role role, Pageable pageable);

    // 끼니 알림 스케줄러 발송 후보: FCM 토큰이 있고 마스터 알림이 켜진 사용자 (끼니별 토글은 코드에서 필터)
    List<User> findByFcmTokenIsNotNullAndPushNotificationEnabledTrue();
}