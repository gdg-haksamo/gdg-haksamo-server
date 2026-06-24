package com.gdg.haksamo.domain.recommendation.gemini;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * dev/로컬용. 실제 Gemini를 호출하지 않고 후보 앞쪽에서 결정론적으로 추천을 만든다.
 * → GEMINI_API_KEY 없이도 추천 조회·새로고침 흐름 전체를 테스트할 수 있다.
 */
@Slf4j
@Component
@Profile("!prod")
public class StubGeminiClient implements GeminiClient {

    @Override
    public List<GeminiPick> recommend(GeminiRecommendationRequest request) {
        String keywords = request.likedKeywords().isEmpty()
                ? null
                : String.join(", ", request.likedKeywords());

        int size = Math.min(request.count(), request.candidates().size());
        List<GeminiPick> picks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            GeminiCandidate c = request.candidates().get(i);
            String reason = (keywords != null)
                    ? "선호하시는 '" + keywords + "' 취향을 고려해 골라봤어요."
                    : "오늘 학식 중 균형 잡힌 한 끼로 추천해요.";
            picks.add(new GeminiPick(c.index(), reason));
        }
        log.info("[DEV-GEMINI] 스텁 추천 {}건 생성 (실제 호출 안 함)", picks.size());
        return picks;
    }
}
