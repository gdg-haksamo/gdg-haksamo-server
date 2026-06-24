package com.gdg.haksamo.domain.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface EventRepository extends JpaRepository<Event, Long> {

    // 날짜 미지정(null)이면 무기한으로 보고, 오늘이 시작일~종료일 사이인 이벤트 개수
    @Query("select count(e) from Event e "
            + "where (e.startDate is null or e.startDate <= :today) "
            + "and (e.endDate is null or e.endDate >= :today)")
    long countActiveEvents(@Param("today") LocalDate today);
}
