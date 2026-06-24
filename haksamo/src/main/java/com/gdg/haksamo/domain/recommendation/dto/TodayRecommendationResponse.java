package com.gdg.haksamo.domain.recommendation.dto;

import com.gdg.haksamo.domain.menu.dto.NutritionResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "오늘의 AI 추천 응답 (현재 보여줄 추천 1건 + 새로고침 잔여)")
public record TodayRecommendationResponse(

        @Schema(description = "메뉴 ID", example = "12")
        Long menuId,

        @Schema(description = "메뉴명", example = "제육볶음")
        String menuName,

        @Schema(description = "식당명", example = "복지관")
        String restaurant,

        @Schema(description = "가격", example = "5500")
        Integer price,

        @Schema(description = "메뉴 분류(AI 생성, 없으면 null)", example = "한식")
        String category,

        @Schema(description = "메뉴 사진 URL(AI 생성, 미구현 시 null)")
        String imageUrl,

        @Schema(description = "메뉴 한 줄 설명(AI 생성, 미구현 시 null)")
        String description,

        @Schema(description = "영양정보")
        NutritionResponse nutrition,

        @Schema(description = "AI 추천 이유 (한 문장)", example = "선호하시는 '매운 음식' 취향을 고려해 골라봤어요.")
        String reason,

        @Schema(description = "추천 기준 날짜", example = "2026-06-25")
        LocalDate date,

        @Schema(description = "오늘 사용한 새로고침 횟수", example = "0")
        int refreshCount,

        @Schema(description = "남은 새로고침 가능 횟수(최대 3, 후보 수에 따라 줄 수 있음)", example = "3")
        int refreshRemaining
) {
}
