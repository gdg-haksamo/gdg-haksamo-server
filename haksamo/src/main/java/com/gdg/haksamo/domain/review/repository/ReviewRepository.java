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
}
