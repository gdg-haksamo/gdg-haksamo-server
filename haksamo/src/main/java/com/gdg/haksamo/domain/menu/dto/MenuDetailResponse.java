package com.gdg.haksamo.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "메뉴 상세 조회 응답")
public record MenuDetailResponse(

        @Schema(description = "메뉴명", example = "제육볶음")
        String menuName,

        @Schema(description = "가격", example = "5500")
        Integer price,

        @Schema(description = "식당명", example = "복지관")
        String restaurant,

        @Schema(description = "운영시간", example = "11:00~13:30")
        String operatingTime,

        @Schema(description = "품절 여부", example = "false")
        boolean soldOut,

        @Schema(description = "메뉴 설명 (AI 생성, 미구현 시 null)")
        String description,

        @Schema(description = "메뉴 사진 URL (AI 생성, 미구현 시 null)")
        String imageUrl,

        @Schema(description = "영양정보")
        NutritionResponse nutrition,

        @Schema(description = "평균 별점 (소수점 1자리, 리뷰 없으면 null)", example = "4.3")
        Double averageRating,

        @Schema(description = "리뷰 개수", example = "12")
        long reviewCount,

        @Schema(description = "인기 리뷰 (도움됐어요 많은 순) 최대 3개")
        List<PopularReviewResponse> popularReviews
) {
}
