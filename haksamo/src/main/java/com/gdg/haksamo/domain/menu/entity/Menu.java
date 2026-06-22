package com.gdg.haksamo.domain.menu.entity;

import com.gdg.haksamo.domain.restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long menuId;

    //Restaurant.java 엔티티 추가 후 수정하기
    private String restaurant;
    private String name;
    private int price;
    private String operatingTime;

    private String category;
    private String imageUrl;
    private String description;
    private Integer calories;
    private Integer protein;
    private Integer carb;
    private Integer fat;
}
