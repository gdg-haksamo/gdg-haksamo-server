package com.gdg.haksamo.global.security;

import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 식당 단위 관리 권한 검사기.
 *
 * <p>메뉴 품절 토글 / 신메뉴 등록 / 이름·가격 수정 / 리뷰 관리 등
 * "특정 식당에 속한 자원"을 변경하는 API에서 호출한다.
 * 권한 규칙은 한 곳(여기)에 모아 도메인마다 흩어지지 않게 한다.
 *
 * <ul>
 *   <li>SUPER_ADMIN(운영팀) — 모든 식당 허용</li>
 *   <li>RESTAURANT_ADMIN(식당 운영자) — 본인 담당 식당(managedRestaurantId)만 허용</li>
 *   <li>그 외(USER) — 거부(403)</li>
 * </ul>
 *
 * <p>사용 예 (채윤님 메뉴/리뷰 서비스):
 * <pre>{@code
 *   // 품절 토글 전에:
 *   restaurantAdminGuard.requirePermission(userId, menu.getRestaurant().getRestaurantId());
 * }</pre>
 *
 * 거부 시 {@link BusinessException}({@link ErrorCode#RESTAURANT_ACCESS_DENIED})을 던지며,
 * GlobalExceptionHandler가 공통 ApiResponse(403)로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class RestaurantAdminGuard {

    private final UserRepository userRepository;

    /**
     * 해당 사용자가 지정한 식당을 관리할 권한이 있는지 검사한다. 없으면 예외를 던진다.
     *
     * @param userId       인증된 사용자 id (@AuthenticationPrincipal Long userId)
     * @param restaurantId 조작 대상 자원이 속한 식당 id
     */
    @Transactional(readOnly = true)
    public void requirePermission(Long userId, Long restaurantId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!user.canManageRestaurant(restaurantId)) {
            throw new BusinessException(ErrorCode.RESTAURANT_ACCESS_DENIED);
        }
    }
}