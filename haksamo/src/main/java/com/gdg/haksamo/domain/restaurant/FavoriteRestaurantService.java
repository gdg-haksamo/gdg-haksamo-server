package com.gdg.haksamo.domain.restaurant;

import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 자주 가는 식당 영속 로직. 회원가입(가입 2단계)과 마이페이지 수정이 공유한다.
 */
@Service
@RequiredArgsConstructor
public class FavoriteRestaurantService {

    private final FavoriteRestaurantRepository favoriteRestaurantRepository;
    private final RestaurantRepository restaurantRepository;

    /**
     * 사용자의 자주 가는 식당을 주어진 ID 목록으로 전체 교체한다.
     * restaurantIds가 null이거나 비어 있으면 기존 즐겨찾기를 모두 제거하고 끝낸다.
     * 중복 ID는 distinct로 정리하고(엔티티에 (user, restaurant) 유니크 제약 존재),
     * 존재하지 않는 ID가 섞이면 RESTAURANT_NOT_FOUND로 거절한다.
     */
    @Transactional
    public void replaceFavorites(User user, List<Long> restaurantIds) {
        favoriteRestaurantRepository.deleteByUser(user);
        if (restaurantIds == null || restaurantIds.isEmpty()) {
            return;
        }
        List<Long> distinctIds = restaurantIds.stream().distinct().toList();
        List<Restaurant> restaurants = restaurantRepository.findAllById(distinctIds);
        if (restaurants.size() != distinctIds.size()) {
            throw new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND);
        }
        List<FavoriteRestaurant> favorites = restaurants.stream()
                .map(restaurant -> FavoriteRestaurant.builder()
                        .user(user)
                        .restaurant(restaurant)
                        .build())
                .toList();
        favoriteRestaurantRepository.saveAll(favorites);
    }
}
