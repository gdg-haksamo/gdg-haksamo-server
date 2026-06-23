package com.gdg.haksamo.domain.user.service;

import com.gdg.haksamo.domain.user.dto.AdminUserListResponse;
import com.gdg.haksamo.domain.user.dto.AdminUserResponse;
import com.gdg.haksamo.domain.user.dto.CreateRestaurantAdminRequest;
import com.gdg.haksamo.domain.user.dto.ResetPasswordRequest;
import com.gdg.haksamo.domain.user.dto.UpdateUserRoleRequest;
import com.gdg.haksamo.domain.user.entity.Role;
import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최상위 관리자(SUPER_ADMIN)의 계정 관리 비즈니스 로직.
 * - 식당 운영자(RESTAURANT_ADMIN) 계정 발급
 * - 권한/담당 식당 변경, 비밀번호 재설정, 계정 삭제
 * 접근 자체는 SecurityConfig의 경로 게이트(/api/admin/** → hasRole SUPER_ADMIN)로 막힌다.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** 전체/권한별 계정 목록 조회. role이 null이면 전체. */
    @Transactional(readOnly = true)
    public AdminUserListResponse getUsers(Role role, Pageable pageable) {
        Page<User> page = (role == null)
                ? userRepository.findAll(pageable)
                : userRepository.findByRole(role, pageable);
        return AdminUserListResponse.from(page.map(AdminUserResponse::from));
    }

    /** 식당 운영자 계정 발급. 이메일 중복 검사 후 RESTAURANT_ADMIN으로 생성. */
    @Transactional
    public AdminUserResponse createRestaurantAdmin(CreateRestaurantAdminRequest request) {
        if (request.restaurantId() == null) {
            throw new BusinessException(ErrorCode.ADMIN_RESTAURANT_REQUIRED);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        // NOTE: 식당 존재 여부 검증은 Restaurant 도메인(채윤님) 머지 후 RestaurantRepository로 추가한다.
        //       (현재 managedRestaurantId는 FK 없는 Long 컬럼 — 조기 결합 회피)
        User saved = userRepository.save(User.createRestaurantAdmin(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                request.restaurantId()));
        return AdminUserResponse.from(saved);
    }

    /** 권한/담당 식당 변경. RESTAURANT_ADMIN으로 바꿀 땐 담당 식당 필수. 본인 계정은 변경 불가(잠금 방지). */
    @Transactional
    public AdminUserResponse updateRole(Long actorUserId, Long targetUserId, UpdateUserRoleRequest request) {
        if (actorUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.ADMIN_CANNOT_MODIFY_SELF);
        }
        if (request.role() == Role.RESTAURANT_ADMIN && request.managedRestaurantId() == null) {
            throw new BusinessException(ErrorCode.ADMIN_RESTAURANT_REQUIRED);
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.changeRole(request.role(), request.managedRestaurantId());
        return AdminUserResponse.from(user);
    }

    /** 비밀번호 재설정 (운영자 분실 대응 등). */
    @Transactional
    public void resetPassword(Long targetUserId, ResetPasswordRequest request) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.resetPassword(passwordEncoder.encode(request.newPassword()));
    }

    /** 계정 삭제. 본인 계정은 삭제 불가(잠금 방지). */
    @Transactional
    public void deleteUser(Long actorUserId, Long targetUserId) {
        if (actorUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.ADMIN_CANNOT_MODIFY_SELF);
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
    }
}