package com.gdg.haksamo.domain.menu.controller;

import com.gdg.haksamo.domain.menu.crawler.MenuCrawlerService;
import com.gdg.haksamo.domain.menu.crawler.MenuInfoEnrichmentService;
import com.gdg.haksamo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 메뉴 크롤 수동 트리거 (운영팀 SUPER_ADMIN 전용).
 * 경로가 /api/admin/** 이라 SecurityConfig 경로 게이트로 SUPER_ADMIN만 접근 가능하다.
 */
@Tag(name = "메뉴 크롤(관리자)")
@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
public class MenuCrawlAdminController {

    private final MenuCrawlerService menuCrawlerService;
    private final MenuInfoEnrichmentService menuInfoEnrichmentService;

    @Operation(
            summary = "신규 메뉴 정보(한줄설명·탄단지) 생성",
            description = "설명·영양값이 비어있는 신규 메뉴만 골라 AI로 생성·저장합니다(이미 채워진 메뉴는 스킵). "
                    + "분당 한도 회피를 위해 청크 사이 간격을 두고 백그라운드에서 비동기 실행합니다(즉시 반환). "
                    + "진행/결과는 서버 로그로 확인하세요. 크롤 시에도 자동 실행됩니다."
    )
    @PostMapping("/enrich-info")
    public ApiResponse<Void> enrichInfo() {
        menuInfoEnrichmentService.enrichMissingAsync();
        return ApiResponse.success();
    }

    @Operation(
            summary = "메뉴 크롤 수동 실행",
            description = "학교 사이트에서 메뉴 편성표를 즉시 크롤링합니다. selDate(yyyy-MM-dd)를 주면 해당 주, "
                    + "생략 시 현재 주를 가져옵니다. 저장은 식당별 wipe & re-insert라 멱등(중복 없음)이며 "
                    + "외부 사이트 6곳을 호출하므로 수 초 소요될 수 있습니다."
    )
    @PostMapping("/crawl")
    public ApiResponse<Void> crawl(
            @Parameter(description = "가져올 주의 날짜 (yyyy-MM-dd, 생략 시 현재 주)", example = "2026-06-15")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selDate
    ) {
        // LocalDate 바인딩으로 형식 검증을 위임 — 잘못된 날짜는 Spring이 400(C001)으로 거부.
        // (오입력으로 빈 편성표가 파싱돼 식당 스케줄이 wipe 후 미삽입되는 상황 방지)
        menuCrawlerService.crawl(selDate == null ? null : selDate.toString());
        return ApiResponse.success();
    }
}
