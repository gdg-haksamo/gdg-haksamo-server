package com.gdg.haksamo.domain.recommendation.gemini;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * dev/로컬용. 실제 Gemini를 호출하지 않고 결정론적으로 추천을 만든다.
 * → GEMINI_API_KEY 없이도 추천 조회·새로고침 흐름 전체를 테스트할 수 있다.
 * 선호 식당(★) 후보를 앞에 두어 선호 반영을 흉내 낸다.
 */
@Slf4j
@Component
@Profile("!prod")
public class StubGeminiClient implements GeminiClient {

    @Override
    public List<GeminiPick> recommend(GeminiRecommendationRequest request) {
        // 선호 식당(★) 후보 먼저, 그 다음 나머지 — 후보 순서만 바꿔 우선순위를 흉내.
        List<GeminiCandidate> ordered = new ArrayList<>();
        request.candidates().stream().filter(GeminiCandidate::favorite).forEach(ordered::add);
        request.candidates().stream().filter(c -> !c.favorite()).forEach(ordered::add);

        int size = Math.min(request.count(), ordered.size());
        List<GeminiPick> picks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            picks.add(new GeminiPick(ordered.get(i).index()));
        }
        log.info("[DEV-GEMINI] 스텁 추천 {}건 생성 (실제 호출 안 함)", picks.size());
        return picks;
    }
}
