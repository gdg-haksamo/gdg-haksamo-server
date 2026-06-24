package com.gdg.haksamo.domain.recommendation.gemini;

/**
 * Gemini에 넘기는 추천 후보 메뉴 1건.
 *
 * <p>{@code index}는 응답({@link GeminiPick})과 메뉴를 매칭하는 키다.
 * 메뉴명 문자열로 되돌려 매칭하면 표기 차이로 깨질 수 있어, 서버가 부여한 index로 주고받는다.
 * {@code favorite}는 사용자의 선호 식당 소속 메뉴인지 — 프롬프트에서 우선 고려 신호로 쓴다.
 */
public record GeminiCandidate(
        int index,
        Long menuId,
        String name,
        String restaurant,
        boolean favorite,
        Integer calories,
        Integer protein,
        Integer carb,
        Integer fat
) {
}
