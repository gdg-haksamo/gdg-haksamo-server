package com.gdg.haksamo.domain.recommendation.gemini;

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
        List<GeminiPick> picks = DeterministicPicker.pick(request);
        log.info("[DEV-GEMINI] 스텁 추천 {}건 생성 (실제 호출 안 함)", picks.size());
        return picks;
    }
}
