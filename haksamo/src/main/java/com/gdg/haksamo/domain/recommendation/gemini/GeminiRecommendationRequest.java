package com.gdg.haksamo.domain.recommendation.gemini;

import java.util.List;

/**
 * 추천 1회 호출 입력.
 *
 * @param mealLabel           끼니 라벨(아침/점심/저녁) — 프롬프트 문맥
 * @param candidates          해당 끼니의 후보 메뉴(품절 제외, 중복 제거됨)
 * @param likedKeywords       사용자 선호 키워드 라벨. 없으면 빈 리스트(일반 추천).
 * @param favoriteRestaurants 사용자 선호 식당명. 없으면 빈 리스트.
 * @param count               미리 받아둘 추천 개수(shortlist 크기)
 */
public record GeminiRecommendationRequest(
        String mealLabel,
        List<GeminiCandidate> candidates,
        List<String> likedKeywords,
        List<String> favoriteRestaurants,
        int count
) {
}
