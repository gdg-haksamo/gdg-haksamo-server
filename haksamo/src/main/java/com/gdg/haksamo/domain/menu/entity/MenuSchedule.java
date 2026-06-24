package com.gdg.haksamo.domain.menu.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class MenuSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;

    @ManyToOne
    @JoinColumn(name = "menu_id")   //DB 컬럼명
    private Menu menu;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private MealTime time;

    private String operatingTime;

    private boolean isSoldOut = false;
}
