package com.gdg.haksamo.domain.menu.ai;

/**
 * 메뉴 정보 생성 입력 1건. {@code menuId}로 응답을 매칭한다(위치 의존 금지 → 누락/순서변경에 안전).
 */
public record MenuInfoTarget(
        Long menuId,
        String name,
        String restaurant
) {
}
