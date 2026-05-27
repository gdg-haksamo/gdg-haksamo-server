package com.gdg.haksamo.domain.menu;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String restaurant;
    private String week;
    @Enumerated(EnumType.STRING)
    private MealTime time;
    private int price;
    private boolean special;
}
