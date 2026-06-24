package com.gdg.haksamo.domain.recommendation.entity;

import com.gdg.haksamo.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 오늘의 AI 추천 헤더. (user_id, date) 당 1행만 유지(캐시).
 *
 * <p>첫 조회 시 Gemini를 <b>1회만</b> 호출해 추천 후보 여러 개(shortlist)를 미리 받아
 * {@link RecommendationMenu}로 저장한다. 새로고침은 Gemini를 다시 부르지 않고
 * 미리 받아둔 다음 후보를 DB에서 꺼내 반환한다 → 실시간 응답·추가 비용 0.
 *
 * <p>{@code refreshCount}는 "오늘 사용한 새로고침 횟수"이자 "현재 보여줄 후보의 위치(0-based 포인터)"를 겸한다.
 */
@Entity
@Getter
@Table(
        name = "Recommendation",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_recommendation_user_date",
                columnNames = {"user_id", "date"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate date;

    /** 오늘 사용한 새로고침 횟수이자 현재 보여줄 후보의 인덱스(0-based). */
    @Column(name = "refresh_count", nullable = false)
    private int refreshCount;

    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<RecommendationMenu> menus = new ArrayList<>();

    private Recommendation(Long userId, LocalDate date) {
        this.userId = userId;
        this.date = date;
        this.refreshCount = 0;
    }

    public static Recommendation create(Long userId, LocalDate date) {
        return new Recommendation(userId, date);
    }

    public void addMenu(RecommendationMenu menu) {
        menu.assignTo(this);
        this.menus.add(menu);
    }

    /** 현재(또는 새로고침 후) 사용자에게 보여줄 후보. shortlist 범위를 벗어나지 않도록 보정한다. */
    public RecommendationMenu currentMenu() {
        int index = Math.min(refreshCount, menus.size() - 1);
        return menus.get(index);
    }

    /** 오늘 가능한 새로고침 최대 횟수. 미리 받아둔 후보 수와 하루 한도 중 작은 값. */
    public int maxRefresh(int dailyLimit) {
        return Math.min(dailyLimit, menus.size() - 1);
    }

    public boolean canRefresh(int dailyLimit) {
        return refreshCount < maxRefresh(dailyLimit);
    }

    public void refresh() {
        this.refreshCount++;
    }
}
