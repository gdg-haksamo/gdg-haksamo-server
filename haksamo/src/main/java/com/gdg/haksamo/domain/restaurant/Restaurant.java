package com.gdg.haksamo.domain.restaurant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long restaurantId;

    private Double mapX;
    private Double mapY;
    private String operatingTime;

    @Column(nullable = false, unique = true)
    private String name;
}
