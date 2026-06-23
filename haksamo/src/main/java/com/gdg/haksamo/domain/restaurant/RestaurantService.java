package com.gdg.haksamo.domain.restaurant;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;

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
}