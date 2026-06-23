package com.gdg.haksamo.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "끼니별 메뉴 묶음")
public record MealMenusResponse(

        @Schema(description = "메뉴 개수", example = "5")
        int count,

        @Schema(description = "메뉴 목록")
        List<MenuResponse> menus
) {
    public static MealMenusResponse of(List<MenuResponse> menus) {
        return new MealMenusResponse(menus.size(), menus);
    }
}
