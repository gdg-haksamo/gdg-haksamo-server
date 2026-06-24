package com.gdg.haksamo.domain.notification.dto;

import com.gdg.haksamo.domain.recommendation.dto.AdhocRecommendation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 데모 추천 푸시 결과 — 생성된 추천 4개 + 발송 여부.
 * 1순위(rank 0)가 푸시되고, 나머지는 새로고침 시 보일 후보(저장하지 않으므로 참고용).
 */
@Schema(description = "데모 추천 푸시 결과")
public record DemoPushResponse(

        @Schema(description = "실제 FCM 발송 여부(자격증명 미설정 시 로그만 → false)", example = "true")
        boolean pushed,

        @Schema(description = "발송 처리 상세", example = "FCM 발송 완료")
        String detail,

        @Schema(description = "생성된 추천 4개(rank 0이 푸시됨)")
        List<AdhocRecommendation> recommendations
) {
}
