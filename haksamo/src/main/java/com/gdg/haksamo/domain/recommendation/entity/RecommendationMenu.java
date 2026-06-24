package com.gdg.haksamo.domain.recommendation.entity;

import com.gdg.haksamo.domain.menu.entity.Menu;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추천 헤더에 매달리는 개별 추천 메뉴(미리 받아둔 shortlist의 한 칸).
 *
 * <p>{@code displayOrder = 0}이 1순위(첫 화면), 새로고침할수록 다음 순번을 보여준다.
 * {@code reason}은 해당 메뉴에 대한 Gemini 한 줄 추천 이유다.
 * 리뷰 정합성을 위해 영구 카탈로그인 {@link Menu} 기준으로 저장한다.
 */
@Entity
@Getter
@Table(name = "RecommendationMenu")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    /** 추천 순위(0-based). 0 = 첫 화면, 1·2·3 = 새로고침 시 순차 노출. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private RecommendationMenu(Menu menu, int displayOrder, String reason) {
        this.menu = menu;
        this.displayOrder = displayOrder;
        this.reason = reason;
    }

    public static RecommendationMenu of(Menu menu, int displayOrder, String reason) {
        return new RecommendationMenu(menu, displayOrder, reason);
    }

    void assignTo(Recommendation recommendation) {
        this.recommendation = recommendation;
    }
}
