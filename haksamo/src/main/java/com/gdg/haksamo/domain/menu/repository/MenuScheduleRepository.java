package com.gdg.haksamo.domain.menu.repository;

import com.gdg.haksamo.domain.menu.entity.MealTime;
import com.gdg.haksamo.domain.menu.entity.Menu;
import com.gdg.haksamo.domain.menu.entity.MenuSchedule;
import com.gdg.haksamo.domain.restaurant.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MenuScheduleRepository extends JpaRepository<MenuSchedule, Long> {
    Optional<MenuSchedule> findByMenuAndDateAndTime(
            Menu menu,
            LocalDate date,
            MealTime time
    );

    List<MenuSchedule> findByDate(LocalDate date);

    // 관리자 메뉴 조회용 — 특정 식당의 특정 날짜 편성 목록
    List<MenuSchedule> findByMenu_Restaurant_RestaurantIdAndDate(Long restaurantId, LocalDate date);

    Optional<MenuSchedule> findFirstByMenu(Menu menu);

    void deleteByMenu_Restaurant(Restaurant restaurant);

    // 메뉴 삭제 시 연결 편성 정리
    void deleteByMenu(Menu menu);
}