package com.gdg.haksamo.domain.notification.dto;

import com.gdg.haksamo.domain.recommendation.dto.TodayRecommendationResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 추천 푸시 결과 — "무엇을 보냈는지(추천 내용)"와 "실제 발송 여부"를 함께 돌려준다.
 * 추천 내용을 응답에 담아 관리자가 발송 전 내용을 확인할 수 있게 한다.
 */
@Schema(description = "관리자 추천 푸시 결과")
public record RecommendationPushResponse(

        @Schema(description = "대상 사용자 id", example = "1")
        Long userId,

        @Schema(description = "실제 FCM 발송 여부(자격증명 미설정 시 로그만 → false)", example = "true")
        boolean pushed,

        @Schema(description = "발송 처리 상세", example = "FCM 발송 완료")
        String detail,

        @Schema(description = "푸시한(또는 푸시 예정인) 추천 내용")
        TodayRecommendationResponse recommendation
) {
}
