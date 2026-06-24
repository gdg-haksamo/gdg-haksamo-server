package com.gdg.haksamo.domain.menu.repository;

import com.gdg.haksamo.domain.menu.entity.Menu;
import com.gdg.haksamo.domain.restaurant.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    Optional<Menu> findByRestaurantAndName(
            Restaurant restaurant,
            String name
    );

    List<Menu> findByRestaurant(Restaurant restaurant);

    // 메뉴 정보 생성 대상 — 한줄설명이 아직 없는(신규) 메뉴. (description이 채워지면 영양값도 함께 채워진 것)
    List<Menu> findByDescriptionIsNull();
}