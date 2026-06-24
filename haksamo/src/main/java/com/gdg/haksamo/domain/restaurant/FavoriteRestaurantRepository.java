package com.gdg.haksamo.domain.restaurant;

import com.gdg.haksamo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FavoriteRestaurantRepository extends JpaRepository<FavoriteRestaurant, Long> {
    List<FavoriteRestaurant> findByUser(User user);

    // 벌크 삭제로 DELETE를 즉시 DB에 반영한다. 파생 삭제(엔티티 단위 remove)는 flush가 지연돼
    // 같은 트랜잭션의 INSERT가 먼저 나가면서 (user, restaurant) 유니크 제약과 충돌할 수 있다.
    @Modifying
    @Query("delete from FavoriteRestaurant f where f.user = :user")
    void deleteByUser(@Param("user") User user);
}
