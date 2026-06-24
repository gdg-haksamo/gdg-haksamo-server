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
}