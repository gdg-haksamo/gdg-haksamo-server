package com.gdg.haksamo.domain.menu.crawler;

import com.gdg.haksamo.domain.menu.ai.GeneratedMenuInfo;
import com.gdg.haksamo.domain.menu.ai.MenuInfoGenerator;
import com.gdg.haksamo.domain.menu.ai.MenuInfoTarget;
import com.gdg.haksamo.domain.menu.entity.Menu;
import com.gdg.haksamo.domain.menu.repository.MenuRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 신규 메뉴(한줄설명·탄단지 비어있음)에 대해 Gemini로 정보를 생성·저장한다. (ADR-0002, erd-decisions #19)
 *
 * <p>비용 절감: 대상 메뉴를 청크 단위로 모아 한 번에 호출. 응답은 메뉴 id로 매칭(위치 의존 X) →
 * 모델이 순서를 바꾸거나 일부를 누락해도 엉뚱한 메뉴에 값이 들어가지 않는다.
 * 청크별로 별도 트랜잭션에 원자적으로 저장하고, Gemini 호출은 트랜잭션 밖에서 한다(커넥션 점유 방지).
 * 크롤 본체와 분리 — 한 청크 실패가 크롤·다른 청크에 영향을 주지 않는다.
 */
@Slf4j
@Service
public class MenuInfoEnrichmentService {

    private static final int CHUNK_SIZE = 20; // 한 호출당 메뉴 수 — 응답 잘림 방지

    private final MenuRepository menuRepository;
    private final MenuInfoGenerator generator;
    private final TransactionTemplate tx;
    /** 청크 호출 사이 간격(ms). Gemini 분당 한도(무료 ~15RPM) 아래로 깔기 위함. 5000ms → 최대 ~12회/분. */
    private final long throttleMs;

    public MenuInfoEnrichmentService(MenuRepository menuRepository, MenuInfoGenerator generator,
            PlatformTransactionManager transactionManager,
            @Value("${gemini.menu-info.throttle-ms:5000}") long throttleMs) {
        this.menuRepository = menuRepository;
        this.generator = generator;
        this.tx = new TransactionTemplate(transactionManager);
        this.throttleMs = throttleMs;
    }

    /** 비동기 트리거 — 크롤/관리자 트리거가 블로킹되지 않게 백그라운드에서 실행한다. */
    @Async
    public void enrichMissingAsync() {
        try {
            enrichMissing();
        } catch (Exception e) {
            log.error("[메뉴정보] 비동기 생성 실패", e);
        }
    }

    /** 한줄설명이 비어있는 메뉴를 청크로 나눠 생성·저장한다. 채워진 메뉴 수를 반환. */
    public int enrichMissing() {
        List<Long> ids = menuRepository.findByDescriptionIsNull().stream()
                .map(Menu::getMenuId)
                .toList();
        if (ids.isEmpty()) {
            log.info("[메뉴정보] 생성 대상 없음");
            return 0;
        }

        int total = 0;
        for (int i = 0; i < ids.size(); i += CHUNK_SIZE) {
            if (i > 0) {
                throttle(); // 분당 한도 회피 — 청크 호출 사이 간격
            }
            List<Long> chunk = ids.subList(i, Math.min(i + CHUNK_SIZE, ids.size()));
            try {
                total += enrichChunk(chunk);
            } catch (Exception e) {
                log.warn("[메뉴정보] 청크 생성 실패(스킵): {}", e.toString());
            }
        }
        log.info("[메뉴정보] 생성 완료 {}/{}건", total, ids.size());
        return total;
    }

    private void throttle() {
        if (throttleMs <= 0) {
            return;
        }
        try {
            Thread.sleep(throttleMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private int enrichChunk(List<Long> ids) {
        // 1) 읽기 트랜잭션 — 타깃 구성(식당명 lazy 접근을 트랜잭션 안에서)
        List<MenuInfoTarget> targets = tx.execute(status ->
                menuRepository.findAllById(ids).stream()
                        .map(menu -> new MenuInfoTarget(
                                menu.getMenuId(), menu.getName(),
                                menu.getRestaurant() != null ? menu.getRestaurant().getName() : null))
                        .toList());

        // 2) Gemini 호출 — 트랜잭션 밖
        List<GeneratedMenuInfo> generated = generator.generate(targets);
        if (generated.isEmpty()) {
            return 0;
        }
        Map<Long, GeneratedMenuInfo> byId = generated.stream()
                .collect(Collectors.toMap(GeneratedMenuInfo::menuId, Function.identity(), (a, b) -> a));

        // 3) 쓰기 트랜잭션 — id로 매칭된 메뉴만 적용(원자적). 매칭 안 된 메뉴는 건드리지 않음.
        Integer applied = tx.execute(status -> {
            int n = 0;
            for (Menu menu : menuRepository.findAllById(ids)) {
                GeneratedMenuInfo info = byId.get(menu.getMenuId());
                if (info == null) {
                    continue;
                }
                menu.setDescription(info.description());
                menu.setCalories(info.calories());
                menu.setProtein(info.protein());
                menu.setCarb(info.carb());
                menu.setFat(info.fat());
                n++;
            }
            return n; // dirty checking으로 커밋 시 저장
        });
        return applied != null ? applied : 0;
    }
}
