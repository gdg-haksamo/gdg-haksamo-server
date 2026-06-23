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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    private String name;
    private Integer price;

    private String category;
    private String imageUrl;
    private String description;
    private Integer calories;
    private Integer protein;
    private Integer carb;
    private Integer fat;
}
