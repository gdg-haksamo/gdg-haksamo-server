package com.gdg.haksamo.domain.menu.dto;

import com.gdg.haksamo.domain.menu.entity.Menu;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 신메뉴 등록 / 이름·가격 수정 결과 응답.
 */
@Schema(description = "메뉴 관리(등록·수정) 결과")
public record MenuAdminResponse(

        @Schema(description = "메뉴 id", example = "10")
        Long menuId,

        @Schema(description = "식당 id", example = "1")
        Long restaurantId,

        @Schema(description = "메뉴 이름", example = "제육덮밥")
        String name,

        @Schema(description = "가격(원)", example = "5000")
        Integer price,

        @Schema(description = "분류", example = "백반")
        String category,

        @Schema(description = "메뉴 이미지 URL(관리자 주입, 없으면 null)", example = "https://.../jeyuk.jpg")
        String imageUrl
) {
    public static MenuAdminResponse from(Menu menu) {
        return new MenuAdminResponse(
                menu.getMenuId(),
                menu.getRestaurant().getRestaurantId(),
                menu.getName(),
                menu.getPrice(),
                menu.getCategory(),
                menu.getImageUrl()
        );
    }
}
