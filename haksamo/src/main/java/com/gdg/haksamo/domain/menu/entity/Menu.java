package com.gdg.haksamo.domain.menu.entity;

import com.gdg.haksamo.domain.restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.*;

@Entity
// (식당, 이름) 식별자 = upsert 기준·리뷰 정합성 방어선 (erd.sql uq_menu_identity).
@Table(uniqueConstraints = @UniqueConstraint(name = "uq_menu_identity", columnNames = {"restaurant_id", "name"}))
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

    // S3/CDN URL은 한글 키 인코딩 등으로 255자를 넘을 수 있어 여유 있게 잡는다. (관리자 이미지 주입 API)
    @Column(length = 512)
    private String imageUrl;

    private String description;
    private Integer calories;
    private Integer protein;
    private Integer carb;
    private Integer fat;
}
