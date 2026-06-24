package com.gdg.haksamo.domain.restaurant;

import com.gdg.haksamo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRestaurantRepository extends JpaRepository<FavoriteRestaurant, Long> {
    List<FavoriteRestaurant> findByUser(User user);

    // 선호 식당은 유저당 1개지만, 단일화 이전 레거시 다중행이 남아 있을 수 있다.
    // id 오름차순 첫 행만 취해 읽기를 결정론적으로 만든다(DB 반환 순서 의존 제거).
    Optional<FavoriteRestaurant> findFirstByUserOrderByIdAsc(User user);

    // 벌크 삭제로 DELETE를 즉시 DB에 반영한다. 파생 삭제(엔티티 단위 remove)는 flush가 지연돼
    // 같은 트랜잭션의 INSERT가 먼저 나가면서 (user, restaurant) 유니크 제약과 충돌할 수 있다.
    @Modifying
    @Query("delete from FavoriteRestaurant f where f.user = :user")
    void deleteByUser(@Param("user") User user);
}
