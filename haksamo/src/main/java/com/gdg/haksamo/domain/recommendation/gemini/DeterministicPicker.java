package com.gdg.haksamo.domain.recommendation.gemini;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemini 없이(또는 실패 시) 결정론적으로 후보를 고른다 — 선호 식당(★) 먼저, 그다음 편성 순서.
 * dev 스텁과 prod 폴백(한도초과·오류 시)이 공유한다. 후보 index만 고르므로 없는 메뉴를 만들지 않는다.
 */
final class DeterministicPicker {

    private DeterministicPicker() {
    }

    static List<GeminiPick> pick(GeminiRecommendationRequest request) {
        List<GeminiCandidate> ordered = new ArrayList<>();
        request.candidates().stream().filter(GeminiCandidate::favorite).forEach(ordered::add);
        request.candidates().stream().filter(c -> !c.favorite()).forEach(ordered::add);

        int size = Math.min(request.count(), ordered.size());
        List<GeminiPick> picks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            picks.add(new GeminiPick(ordered.get(i).index()));
        }
        return picks;
    }
}
