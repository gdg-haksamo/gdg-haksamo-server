package com.gdg.haksamo.domain.menu.ai;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * dev/로컬용. 실제 호출 없이 결정론적으로 한줄설명·탄단지를 채운다 — 생성·저장 흐름 테스트용.
 */
@Slf4j
@Component
@Profile("!prod")
public class StubMenuInfoGenerator implements MenuInfoGenerator {

    @Override
    public List<GeneratedMenuInfo> generate(List<MenuInfoTarget> targets) {
        List<GeneratedMenuInfo> result = new ArrayList<>(targets.size());
        for (MenuInfoTarget t : targets) {
            result.add(new GeneratedMenuInfo(
                    t.menuId(),
                    t.name() + ", 오늘도 든든한 한 끼예요.",
                    600, 20, 70, 15));
        }
        log.info("[DEV-MENU-AI] 스텁 메뉴정보 {}건 생성 (실제 호출 안 함)", result.size());
        return result;
    }
}
