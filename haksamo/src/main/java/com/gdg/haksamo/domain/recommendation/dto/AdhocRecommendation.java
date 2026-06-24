package com.gdg.haksamo.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 무상태(저장 안 함) 추천 1건 — 데모용 입력 기반 생성 결과.
 * rank 0 = 1순위(푸시 대상), reason은 확인용(푸시 문구엔 미포함).
 */
@Schema(description = "데모 추천 결과 1건")
public record AdhocRecommendation(

        @Schema(description = "추천 순위(0=1순위)", example = "0")
        int rank,

        @Schema(description = "메뉴 id", example = "12")
        Long menuId,

        @Schema(description = "메뉴명", example = "매운제육덮밥")
        String menuName,

        @Schema(description = "식당명", example = "복지관")
        String restaurant,

        @Schema(description = "추천 이유(확인용)", example = "선호하시는 '매운 음식' 취향을 고려했어요.")
        String reason
) {
}
