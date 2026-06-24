package com.gdg.haksamo.domain.mypage.dto;

import com.gdg.haksamo.domain.preference.PreferenceKeyword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "선호 키워드 수정 요청 (전체 교체)")
public record PreferencesUpdateRequest(

        @Schema(description = "선호 키워드 코드 목록 (이 목록으로 전체 교체됨, GET /preferences/keywords 참고)", example = "[\"SPICY\",\"KOREAN\"]")
        @NotNull
        @Size(max = PreferenceKeyword.MAX_SELECTION, message = "선호 키워드는 최대 {max}개까지 선택할 수 있습니다.")
        List<PreferenceKeyword> keywords
) {
}
