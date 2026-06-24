package com.gdg.haksamo.domain.menu.controller;

import com.gdg.haksamo.domain.menu.crawler.MenuCrawlerService;
import com.gdg.haksamo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @Operation(
            summary = "메뉴 크롤 수동 실행",
            description = "학교 사이트에서 메뉴 편성표를 즉시 크롤링합니다. selDate(yyyy-MM-dd)를 주면 해당 주, "
                    + "생략 시 현재 주를 가져옵니다. 저장은 식당별 wipe & re-insert라 멱등(중복 없음)이며 "
                    + "외부 사이트 6곳을 호출하므로 수 초 소요될 수 있습니다."
    )
    @PostMapping("/crawl")
    public ApiResponse<Void> crawl(
            @Parameter(description = "가져올 주의 날짜 (yyyy-MM-dd, 생략 시 현재 주)", example = "2026-06-15")
            @RequestParam(required = false) String selDate
    ) {
        menuCrawlerService.crawl(selDate);
        return ApiResponse.success();
    }
}
