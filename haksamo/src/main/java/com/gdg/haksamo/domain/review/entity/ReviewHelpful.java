package com.gdg.haksamo.domain.review.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// userId의 물리 컬럼명은 user_id — 제약 컬럼명을 실제 컬럼(user_id)에 맞춰야 UNIQUE가 형성된다.
@Table(uniqueConstraints = @UniqueConstraint(name = "uq_review_helpful_user", columnNames = {"review_id", "user_id"}))
public class ReviewHelpful {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    // User FK는 MVP에선 의도적으로 미연결(느슨한 결합). @ManyToOne User 전환은 후속 작업.
    private Long userId;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
