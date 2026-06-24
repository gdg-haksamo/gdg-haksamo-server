package com.gdg.haksamo.domain.recommendation.gemini;

/**
 * Gemini 추천 1건. {@code index}는 요청한 {@link GeminiCandidate#index()}와 매칭된다.
 * 응답 순서가 곧 추천 순위(앞일수록 우선). 메뉴는 후보 index로만 받으므로 모델이 없는 메뉴를 지어낼 수 없다.
 */
public record GeminiPick(
        int index
) {
}
