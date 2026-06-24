package com.gdg.haksamo.domain.recommendation.entity;

import com.gdg.haksamo.domain.menu.entity.MealTime;
import com.gdg.haksamo.global.common.BaseTimeEntity;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
                name = "uq_recommendation_user_date_meal",
                columnNames = {"user_id", "date", "meal"}))
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

    /** 끼니(아침/점심/저녁). (user_id, date, meal) 당 1행. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealTime meal;

    /** 현재 보여줄 후보의 위치(0-based 포인터). 새로고침마다 후보 수 기준으로 순환한다. */
    @Column(name = "refresh_count", nullable = false)
    private int refreshCount;

    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<RecommendationMenu> menus = new ArrayList<>();

    private Recommendation(Long userId, LocalDate date, MealTime meal) {
        this.userId = userId;
        this.date = date;
        this.meal = meal;
        this.refreshCount = 0;
    }

    public static Recommendation create(Long userId, LocalDate date, MealTime meal) {
        return new Recommendation(userId, date, meal);
    }

    public void addMenu(RecommendationMenu menu) {
        menu.assignTo(this);
        this.menus.add(menu);
    }

    /** 현재(또는 새로고침 후) 사용자에게 보여줄 후보. 포인터를 후보 수로 모듈로 보정해 범위 밖 드리프트를 방어한다. */
    public RecommendationMenu currentMenu() {
        // 정상 경로(generate)에선 빈 shortlist를 막지만, 도메인 메서드 단독 안전성을 위해 방어한다.
        if (menus.isEmpty()) {
            throw new BusinessException(ErrorCode.RECOMMENDATION_UNAVAILABLE);
        }
        return menus.get(refreshCount % menus.size());
    }

    /** 미리 받아둔 후보 수(순환 주기). */
    public int candidateCount() {
        return menus.size();
    }

    /** 새로고침: 다음 후보로 포인터 이동. 마지막 후보 다음은 첫 후보로 순환한다(한도·차단 없음). */
    public void refresh() {
        this.refreshCount = (this.refreshCount + 1) % menus.size();
    }
}
