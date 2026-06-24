package com.gdg.haksamo.domain.recommendation.gemini;

import java.util.List;

/**
 * 추천 1회 호출 입력. 후보 메뉴 목록 + 사용자 선호 키워드(LIKED) + 받을 추천 개수.
 *
 * @param candidates    오늘의 후보 메뉴(품절 제외, 중복 제거됨)
 * @param likedKeywords 사용자 선호 키워드 라벨. 없으면 빈 리스트(일반 추천).
 * @param count         미리 받아둘 추천 개수(shortlist 크기)
 */
public record GeminiRecommendationRequest(
        List<GeminiCandidate> candidates,
        List<String> likedKeywords,
        int count
) {
}
