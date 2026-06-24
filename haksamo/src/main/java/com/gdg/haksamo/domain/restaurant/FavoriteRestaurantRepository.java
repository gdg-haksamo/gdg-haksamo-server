package com.gdg.haksamo.domain.restaurant;

import com.gdg.haksamo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRestaurantRepository extends JpaRepository<FavoriteRestaurant, Long> {
    List<FavoriteRestaurant> findByUser(User user);

    void deleteByUser(User user);
}
