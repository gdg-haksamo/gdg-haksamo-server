package com.gdg.haksamo.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 데모용 추천 1건. rank 0 = 1순위(푸시 대상). 메뉴는 실제 후보(MenuSchedule)에서 고른 것.
 */
@Schema(description = "데모 추천 결과 1건")
public record AdhocRecommendation(

        @Schema(description = "추천 순위(0=1순위)", example = "0")
        int rank,

        @Schema(description = "메뉴 id", example = "12")
        Long menuId,

        @Schema(description = "메뉴명", example = "매운제육덮밥")
        String menuName,

        @Schema(description = "식당명(메뉴에 식당 정보가 없으면 null)", example = "복지관", nullable = true)
        String restaurant
) {
}
