package com.gdg.haksamo.domain.menu.service;

import com.gdg.haksamo.domain.menu.dto.ManagedMenuResponse;
import com.gdg.haksamo.domain.menu.dto.MenuAdminResponse;
import com.gdg.haksamo.domain.menu.dto.MenuCreateRequest;
import com.gdg.haksamo.domain.menu.dto.MenuUpdateRequest;
import com.gdg.haksamo.domain.menu.entity.Menu;
import com.gdg.haksamo.domain.menu.entity.MenuSchedule;
import com.gdg.haksamo.domain.menu.repository.MenuRepository;
import com.gdg.haksamo.domain.menu.repository.MenuScheduleRepository;
import com.gdg.haksamo.domain.restaurant.Restaurant;
import com.gdg.haksamo.domain.restaurant.RestaurantRepository;
import com.gdg.haksamo.domain.review.repository.ReviewHelpfulRepository;
import com.gdg.haksamo.domain.review.repository.ReviewRepository;
import com.gdg.haksamo.domain.user.entity.Role;
import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import com.gdg.haksamo.global.security.RestaurantAdminGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 식당 관리자(RESTAURANT_ADMIN)·운영팀(SUPER_ADMIN)의 메뉴 관리.
 *
 * <p>조회는 관리자면 어느 식당이든 열람 가능(역할만 확인). 쓰기(품절 토글/등록/수정/삭제)는
 * {@link RestaurantAdminGuard}로 "본인 담당 식당"만 허용한다(타 식당 시도 시 403 A008).
 */
@Service
@RequiredArgsConstructor
public class MenuAdminService {

    private final MenuRepository menuRepository;
    private final MenuScheduleRepository menuScheduleRepository;
    private final RestaurantRepository restaurantRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulRepository reviewHelpfulRepository;
    private final UserRepository userRepository;
    private final RestaurantAdminGuard restaurantAdminGuard;

    /**
     * 관리자용 메뉴 조회. restaurantId를 주면 그 식당을, 생략하면 본인 담당 식당을 본다.
     * (SUPER_ADMIN은 담당 식당이 없으므로 restaurantId를 반드시 지정해야 한다.)
     */
    @Transactional(readOnly = true)
    public List<ManagedMenuResponse> getManagedMenus(Long userId, Long restaurantId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() == Role.USER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Long targetRestaurantId = (restaurantId != null) ? restaurantId : user.getManagedRestaurantId();
        if (targetRestaurantId == null) {
            // SUPER_ADMIN이 대상 식당을 지정하지 않은 경우
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return menuScheduleRepository
                .findByMenu_Restaurant_RestaurantIdAndDate(targetRestaurantId, targetDate)
                .stream()
                .map(ManagedMenuResponse::from)
                .toList();
    }

    /** 품절 토글. 편성(schedule)이 속한 식당을 담당하는 관리자만 가능. */
    @Transactional
    public void toggleSoldOut(Long userId, Long scheduleId) {
        MenuSchedule schedule = menuScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        restaurantAdminGuard.requirePermission(userId, schedule.getMenu().getRestaurant().getRestaurantId());
        schedule.setSoldOut(!schedule.isSoldOut());
    }

    /** 신메뉴 등록 — Menu(카탈로그) + 당일 MenuSchedule(편성) 생성. 같은 식당 동일 이름 중복 방지. */
    @Transactional
    public MenuAdminResponse createMenu(Long userId, MenuCreateRequest request) {
        Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));
        restaurantAdminGuard.requirePermission(userId, restaurant.getRestaurantId());

        menuRepository.findByRestaurantAndName(restaurant, request.name())
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.MENU_NAME_DUPLICATED);
                });

        Menu menu = menuRepository.save(Menu.builder()
                .restaurant(restaurant)
                .name(request.name())
                .price(request.price())
                .category(request.category())
                .build());

        MenuSchedule schedule = new MenuSchedule();
        schedule.setMenu(menu);
        schedule.setDate(LocalDate.now());
        schedule.setTime(request.time());
        schedule.setOperatingTime(restaurant.getOperatingTime());
        menuScheduleRepository.save(schedule);

        return MenuAdminResponse.from(menu);
    }

    /** 이름·가격 수정. 메뉴가 속한 식당을 담당하는 관리자만 가능. */
    @Transactional
    public MenuAdminResponse updateMenu(Long userId, Long menuId, MenuUpdateRequest request) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));
        restaurantAdminGuard.requirePermission(userId, menu.getRestaurant().getRestaurantId());

        if (request.name() != null && !request.name().equals(menu.getName())) {
            // 같은 식당 내 동일 이름 중복 방지 — UNIQUE(restaurant, name) 불변식(리뷰 정합성) 유지. 본인은 제외.
            menuRepository.findByRestaurantAndName(menu.getRestaurant(), request.name())
                    .filter(existing -> !existing.getMenuId().equals(menu.getMenuId()))
                    .ifPresent(existing -> {
                        throw new BusinessException(ErrorCode.MENU_NAME_DUPLICATED);
                    });
            menu.setName(request.name());
        }
        if (request.price() != null) {
            menu.setPrice(request.price());
        }
        return MenuAdminResponse.from(menu);
    }

    /**
     * 메뉴 삭제. 메뉴가 속한 식당을 담당하는 관리자만 가능.
     * 연결된 도움됐어요 → 리뷰 → 편성 순으로 정리한 뒤 메뉴를 삭제한다.
     * (크롤링 대상 메뉴는 다음 주 크롤에서 다시 생성될 수 있음 — 운영 메모)
     */
    @Transactional
    public void deleteMenu(Long userId, Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));
        restaurantAdminGuard.requirePermission(userId, menu.getRestaurant().getRestaurantId());

        reviewHelpfulRepository.deleteByReview_Menu(menu);
        reviewRepository.deleteByMenu(menu);
        menuScheduleRepository.deleteByMenu(menu);
        menuRepository.delete(menu);
    }
}
