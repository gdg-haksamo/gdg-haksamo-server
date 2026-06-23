package com.gdg.haksamo.domain.menu.controller;

import com.gdg.haksamo.domain.menu.dto.MenuResponse;
import com.gdg.haksamo.domain.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/menus")
public class MenuController {
    private final MenuService menuService;

    @GetMapping
    public List<MenuResponse> getMenus(@RequestParam LocalDate date) {
        return menuService.getMenus(date);
    }
}