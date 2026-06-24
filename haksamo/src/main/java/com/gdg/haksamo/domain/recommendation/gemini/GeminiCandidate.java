package com.gdg.haksamo.domain.recommendation.gemini;

/**
 * Gemini에 넘기는 추천 후보 메뉴 1건.
 *
 * <p>{@code index}는 응답({@link GeminiPick})과 메뉴를 매칭하는 키다.
 * 메뉴명 문자열로 되돌려 매칭하면 표기 차이로 깨질 수 있어, 서버가 부여한 index로 주고받는다.
 */
public record GeminiCandidate(
        int index,
        Long menuId,
        String name,
        String restaurant,
        Integer calories,
        Integer protein,
        Integer carb,
        Integer fat
) {
}
