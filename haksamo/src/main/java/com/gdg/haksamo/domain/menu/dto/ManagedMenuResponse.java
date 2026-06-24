package com.gdg.haksamo.domain.menu.dto;

import com.gdg.haksamo.domain.menu.entity.MealTime;
import com.gdg.haksamo.domain.menu.entity.MenuSchedule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 관리자 메뉴 관리 화면용 응답. 품절 토글에 필요한 scheduleId·isSoldOut을 포함한다.
 */
@Schema(description = "관리자 메뉴 관리용 편성 항목")
public record ManagedMenuResponse(

        @Schema(description = "편성(스케줄) id — 품절 토글 대상", example = "100")
        Long scheduleId,

        @Schema(description = "메뉴 id — 이름·가격 수정/삭제 대상", example = "10")
        Long menuId,

        @Schema(description = "메뉴 이름", example = "제육덮밥")
        String name,

        @Schema(description = "가격(원)", example = "5000")
        Integer price,

        @Schema(description = "끼니", example = "LUNCH")
        MealTime time,

        @Schema(description = "날짜", example = "2026-06-24")
        LocalDate date,

        @Schema(description = "품절 여부", example = "false")
        boolean soldOut
) {
    public static ManagedMenuResponse from(MenuSchedule schedule) {
        return new ManagedMenuResponse(
                schedule.getScheduleId(),
                schedule.getMenu().getMenuId(),
                schedule.getMenu().getName(),
                schedule.getMenu().getPrice(),
                schedule.getTime(),
                schedule.getDate(),
                schedule.isSoldOut()
        );
    }
}
