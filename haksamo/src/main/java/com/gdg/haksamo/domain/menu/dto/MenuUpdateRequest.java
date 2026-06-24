package com.gdg.haksamo.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 메뉴 이름·가격 수정 요청. 보낸 필드만 변경(null은 미변경).
 */
public record MenuUpdateRequest(

        @Schema(description = "변경할 메뉴 이름(미변경 시 생략)", example = "수요일 제육덮밥")
        @Size(max = 100, message = "메뉴 이름은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "변경할 가격(원, 미변경 시 생략)", example = "5500")
        @PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
        Integer price
) {
}
