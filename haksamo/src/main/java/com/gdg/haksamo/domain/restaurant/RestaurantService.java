package com.gdg.haksamo.domain.restaurant;

import com.gdg.haksamo.domain.menu.entity.Menu;
import com.gdg.haksamo.domain.menu.repository.MenuRepository;
import com.gdg.haksamo.domain.restaurant.dto.RestaurantMenuResponse;
import com.gdg.haksamo.domain.restaurant.dto.RestaurantResponse;
import com.gdg.haksamo.global.exception.NotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final MenuRepository menuRepository;

    @PostConstruct
    public void initRestaurant() {
        if (restaurantRepository.count() == 0) {
            restaurantRepository.save(Restaurant.builder().name("정보센터").build());
            restaurantRepository.save(Restaurant.builder().name("복지관").build());
            restaurantRepository.save(Restaurant.builder().name("첨성").build());
            restaurantRepository.save(Restaurant.builder().name("글로벌플라자").build());
            restaurantRepository.save(Restaurant.builder().name("공식당 학생식당").build());
            restaurantRepository.save(Restaurant.builder().name("공식당 교직원식당").build());
        }
    }

    public List<RestaurantResponse> getRestaurants() {
        return restaurantRepository.findAll()
                .stream()
                .map(restaurant -> new RestaurantResponse(
                        restaurant.getRestaurantId(),
                        restaurant.getName()
                ))
                .toList();
    }

    public List<RestaurantMenuResponse> getMenus(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("식당을 찾을 수 없습니다: " + restaurantId));

        return menuRepository.findByRestaurant(restaurant)
                .stream()
                .map(this::toRestaurantMenuResponse)
                .toList();
    }

    private RestaurantMenuResponse toRestaurantMenuResponse(Menu menu) {
        return new RestaurantMenuResponse(
                menu.getMenuId(),
                menu.getName(),
                menu.getPrice()
        );
    }
}
