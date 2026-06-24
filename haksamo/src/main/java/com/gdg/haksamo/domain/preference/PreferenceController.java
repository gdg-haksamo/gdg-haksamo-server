package com.gdg.haksamo.domain.preference;

import com.gdg.haksamo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "선호 키워드")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/preferences")
public class PreferenceController {

    @Operation(summary = "선호 키워드 선택지 조회", description = "마이페이지 선호 키워드 수정 화면에서 고를 수 있는 전체 키워드 목록(카테고리별)을 조회합니다.")
    @GetMapping("/keywords")
    public ApiResponse<List<PreferenceKeywordResponse>> getKeywords() {
        return ApiResponse.success(
                List.of(PreferenceKeyword.values())
                        .stream()
                        .map(PreferenceKeywordResponse::from)
                        .toList()
        );
    }
}
