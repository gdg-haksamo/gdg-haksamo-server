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

    void deleteByMenu_Restaurant(Restaurant restaurant);
}