package com.gdg.haksamo.domain.restaurant;

import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 자주 가는 식당 영속 로직. 회원가입(가입 2단계)과 마이페이지 수정이 공유한다.
 * 선호 식당은 유저당 1개(단일)다.
 */
@Service
@RequiredArgsConstructor
public class FavoriteRestaurantService {

    private final FavoriteRestaurantRepository favoriteRestaurantRepository;
    private final RestaurantRepository restaurantRepository;

    /**
     * 사용자의 자주 가는 식당을 주어진 ID(단일)로 교체한다.
     * restaurantId가 null이면 기존 선호 식당을 제거하고 끝낸다(해제).
     * 존재하지 않는 ID면 RESTAURANT_NOT_FOUND로 거절한다.
     */
    @Transactional
    public void setFavorite(User user, Long restaurantId) {
        favoriteRestaurantRepository.deleteByUser(user);
        if (restaurantId == null) {
            return;
        }
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));
        favoriteRestaurantRepository.save(FavoriteRestaurant.builder()
                .user(user)
                .restaurant(restaurant)
                .build());
    }
}
