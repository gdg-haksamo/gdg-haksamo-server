package com.gdg.haksamo.domain.recommendation.repository;

import com.gdg.haksamo.domain.recommendation.entity.Recommendation;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    /** 오늘 추천 캐시 조회. (user_id, date) 유니크라 단건. */
    Optional<Recommendation> findByUserIdAndDate(Long userId, LocalDate date);
}
