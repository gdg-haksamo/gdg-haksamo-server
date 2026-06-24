package com.gdg.haksamo.domain.review.entity;

import com.gdg.haksamo.domain.menu.entity.Menu;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    // User FK는 MVP에선 의도적으로 미연결(느슨한 결합). 닉네임은 서비스에서 배치 lookup.
    // @ManyToOne User 전환은 후속 작업(#78 참고).
    private Long userId;

    private Integer rating;

    private String content;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
