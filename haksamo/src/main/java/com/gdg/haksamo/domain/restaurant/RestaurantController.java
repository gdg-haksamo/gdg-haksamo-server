package com.gdg.haksamo.domain.restaurant;

import com.gdg.haksamo.domain.restaurant.dto.RestaurantMenuResponse;
import com.gdg.haksamo.domain.restaurant.dto.RestaurantResponse;
import com.gdg.haksamo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "식당")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    @Operation(summary = "식당 목록 조회", description = "리뷰 작성 시 식당 선택 등에 사용하는 전체 식당 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<RestaurantResponse>> getRestaurants() {
        return ApiResponse.success(restaurantService.getRestaurants());
    }

    @Operation(summary = "식당별 메뉴 목록 조회", description = "리뷰 작성 시 메뉴 선택에 사용하는, 해당 식당의 전체 메뉴 목록을 조회합니다.")
    @GetMapping("/{restaurantId}/menus")
    public ApiResponse<List<RestaurantMenuResponse>> getMenus(@PathVariable Long restaurantId) {
        return ApiResponse.success(restaurantService.getMenus(restaurantId));
    }
}
