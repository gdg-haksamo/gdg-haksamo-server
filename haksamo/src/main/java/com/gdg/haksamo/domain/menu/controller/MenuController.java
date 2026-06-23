package com.gdg.haksamo.domain.menu.controller;

import com.gdg.haksamo.domain.menu.dto.MenuResponse;
import com.gdg.haksamo.domain.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "메뉴")
@RestController
@RequiredArgsConstructor
@RequestMapping("/menus")
public class MenuController {

    private final MenuService menuService;

    @Operation(
            summary = "날짜별 메뉴 조회",
            description = "선택한 날짜의 학식 메뉴를 조회합니다."
    )
    @GetMapping
    public List<MenuResponse> getMenus(
            @Parameter(
                    description = "조회 날짜 (yyyy-MM-dd)",
                    example = "2026-06-23"
            )
            @RequestParam LocalDate date
    ) {
        return menuService.getMenus(date);
    }
}