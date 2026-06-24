package com.gdg.haksamo.domain.recommendation.gemini;

import java.util.List;

/**
 * Gemini 추천 호출 추상화. 프로파일별 구현을 주입한다.
 * <ul>
 *   <li>dev/local : {@link StubGeminiClient} — 외부 호출 없이 결정론적 추천(키 불필요)</li>
 *   <li>prod      : {@link RestGeminiClient} — Generative Language REST API 실호출</li>
 * </ul>
 * (이메일 인증의 {@code MailSender}(dev=로그/prod=SMTP) 패턴과 동일)
 */
public interface GeminiClient {

    /**
     * 후보 메뉴 중 {@code count}개를 추천 순서대로 고르고, 각 메뉴의 한 줄 이유를 반환한다.
     * 반환 순서가 곧 추천 순위. 실패 시 빈 리스트가 아니라 예외로 알린다(호출부에서 503 처리).
     */
    List<GeminiPick> recommend(GeminiRecommendationRequest request);
}
