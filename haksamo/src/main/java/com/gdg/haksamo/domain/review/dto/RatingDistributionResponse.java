package com.gdg.haksamo.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "메뉴 별점 분포 응답")
public record RatingDistributionResponse(

        @Schema(description = "전체 리뷰 수")
        long totalCount,

        @Schema(description = "별점(1~5)별 리뷰 개수", example = "{\"1\":0,\"2\":1,\"3\":2,\"4\":5,\"5\":10}")
        Map<Integer, Long> distribution
) {
}
