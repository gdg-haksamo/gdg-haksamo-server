package com.gdg.haksamo.domain.recommendation.gemini;

/**
 * Gemini 추천 1건. {@code index}는 요청한 {@link GeminiCandidate#index()}와 매칭된다.
 * 응답 순서가 곧 추천 순위(앞일수록 우선).
 */
public record GeminiPick(
        int index,
        String reason
) {
}
