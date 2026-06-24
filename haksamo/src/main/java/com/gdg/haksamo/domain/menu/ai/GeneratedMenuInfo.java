package com.gdg.haksamo.domain.menu.ai;

/**
 * 메뉴 정보 생성 결과 1건. {@code menuId}로 요청({@link MenuInfoTarget})과 매칭한다.
 */
public record GeneratedMenuInfo(
        Long menuId,
        String description,
        Integer calories,
        Integer protein,
        Integer carb,
        Integer fat
) {
}
