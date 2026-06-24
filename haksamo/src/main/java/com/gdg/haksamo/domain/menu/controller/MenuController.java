package com.gdg.haksamo.domain.menu.controller;

import com.gdg.haksamo.domain.menu.dto.MenuDetailResponse;
import com.gdg.haksamo.domain.menu.dto.MenusByMealTimeResponse;
import com.gdg.haksamo.domain.menu.service.MenuService;
import com.gdg.haksamo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "메뉴")
@RestController
@RequiredArgsConstructor
@RequestMapping("/menus")
public class MenuController {

    private final MenuService menuService;

    @Operation(
            summary = "날짜별 메뉴 조회",
            description = "선택한 날짜의 학식 메뉴를 아침/점심/저녁으로 묶어서 조회합니다."
    )
    @GetMapping
    public ApiResponse<MenusByMealTimeResponse> getMenus(
            @Parameter(
                    description = "조회 날짜 (yyyy-MM-dd)",
                    example = "2026-06-23"
            )
            @RequestParam LocalDate date
    ) {
        return ApiResponse.success(menuService.getMenus(date));
    }

    @Operation(
            summary = "메뉴 상세 조회",
            description = "메뉴 상세 정보(가격, 운영시간, 영양정보, 평균 별점, 최근 리뷰 3개 등)를 조회합니다."
    )
    @GetMapping("/{menuId}")
    public ApiResponse<MenuDetailResponse> getMenuDetail(@PathVariable Long menuId) {
        return ApiResponse.success(menuService.getMenuDetail(menuId));
    }
}
