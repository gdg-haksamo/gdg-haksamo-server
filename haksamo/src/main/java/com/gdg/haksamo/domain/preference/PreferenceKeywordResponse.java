package com.gdg.haksamo.domain.preference;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "선호 키워드 선택지")
public record PreferenceKeywordResponse(

        @Schema(description = "키워드 코드 (선호 키워드 수정 요청에 그대로 사용)", example = "SPICY")
        String name,

        @Schema(description = "카테고리", example = "맛 취향")
        String category,

        @Schema(description = "화면에 표시할 라벨", example = "매운 음식")
        String label
) {
    public static PreferenceKeywordResponse from(PreferenceKeyword keyword) {
        return new PreferenceKeywordResponse(keyword.name(), keyword.getCategory(), keyword.getLabel());
    }
}
