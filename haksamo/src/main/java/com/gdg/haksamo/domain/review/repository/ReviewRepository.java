package com.gdg.haksamo.domain.review.repository;

import com.gdg.haksamo.domain.menu.entity.Menu;
import com.gdg.haksamo.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMenu(Menu menu);

    long countByMenu(Menu menu);

    @Query("select r.rating, count(r) from Review r where r.menu = :menu group by r.rating")
    List<Object[]> countByRatingGroupedForMenu(@Param("menu") Menu menu);

    @Query("select r.menu.menuId, avg(r.rating) from Review r where r.menu.menuId in :menuIds group by r.menu.menuId")
    List<Object[]> averageRatingByMenuIds(@Param("menuIds") List<Long> menuIds);

    @Query("select avg(r.rating) from Review r where r.menu = :menu")
    Double averageRatingForMenu(@Param("menu") Menu menu);

    long countByUserId(Long userId);

    // 메뉴 삭제 시 연결 리뷰 정리 (도움됐어요 먼저 제거한 뒤 호출)
    void deleteByMenu(Menu menu);
}
