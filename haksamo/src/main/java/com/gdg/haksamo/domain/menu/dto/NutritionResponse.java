package com.gdg.haksamo.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "영양정보 (AI 분석, 미구현 시 전부 null)")
public record NutritionResponse(

        @Schema(description = "칼로리(kcal)", example = "650")
        Integer calories,

        @Schema(description = "단백질(g)", example = "25")
        Integer protein,

        @Schema(description = "탄수화물(g)", example = "80")
        Integer carb,

        @Schema(description = "지방(g)", example = "20")
        Integer fat
) {
}
