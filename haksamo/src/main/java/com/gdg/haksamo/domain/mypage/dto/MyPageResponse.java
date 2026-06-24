package com.gdg.haksamo.domain.mypage.dto;

import com.gdg.haksamo.domain.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "마이페이지 요약 응답")
public record MyPageResponse(

        @Schema(description = "닉네임", example = "경대생")
        String nickname,

        @Schema(description = "학과", example = "컴퓨터학부")
        String department,

        @Schema(description = "사용자 권한 (FE 관리자 페이지 분기용)", example = "USER")
        Role role,

        @Schema(description = "관리하는 식당 id (RESTAURANT_ADMIN만 값, USER/SUPER_ADMIN은 null)", example = "5")
        Long managedRestaurantId,

        @Schema(description = "작성한 리뷰 수", example = "5")
        long reviewCount,

        @Schema(description = "도움됐어요 받은 누계", example = "12")
        long helpfulReceivedCount,

        @Schema(description = "진행 중인 이벤트 수 (오늘이 시작일~종료일 사이인 이벤트 개수)", example = "2")
        Long activeEventCount,

        @Schema(description = "자주 가는 식당 목록")
        List<FavoriteRestaurantResponse> favoriteRestaurants,

        @Schema(description = "선호 키워드 목록", example = "[\"매운음식\",\"한식\"]")
        List<String> preferenceKeywords,

        @Schema(description = "알림 설정")
        NotificationSettingsResponse notificationSettings
) {
}
