package com.gdg.haksamo.domain.menu.ai;

import java.util.List;

/**
 * 메뉴 한줄설명·탄단지 생성 추상화. 프로파일별 구현(dev=스텁 / prod=Gemini 실호출).
 *
 * <p>한 번에 한 청크(여러 메뉴)를 받아 한 번 호출로 생성한다(비용 절감, ADR-0002).
 * 결과는 {@code menuId}로 매칭되며, 일부 메뉴가 누락될 수 있다(호출부에서 매칭된 것만 저장).
 */
public interface MenuInfoGenerator {

    List<GeneratedMenuInfo> generate(List<MenuInfoTarget> targets);
}
