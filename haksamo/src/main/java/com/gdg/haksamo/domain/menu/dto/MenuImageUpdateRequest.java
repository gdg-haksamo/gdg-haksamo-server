package com.gdg.haksamo.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 메뉴 이미지 URL 주입 요청(관리자).
 *
 * <p>데모용 임시책: 크롤 시 image_url은 NULL로 두고, 관리자가 S3/CDN에 올린 이미지의 URL을 직접 주입한다.
 * (서버가 파일 업로드까지 하는 풀버전은 데모 이후 후속 — docs/admin-integration-notes.md 참고)
 */
public record MenuImageUpdateRequest(

        @Schema(description = "메뉴 이미지 URL (S3/CDN). http(s)만 허용",
                example = "https://haksamo-menu-images.s3.ap-northeast-2.amazonaws.com/menu-images/gongsikdang/jeyuk.jpg")
        @NotBlank(message = "이미지 URL을 입력해주세요.")
        @Size(max = 512, message = "이미지 URL은 512자 이하여야 합니다.")
        @Pattern(regexp = "^https?://.+", message = "이미지 URL은 http(s):// 로 시작해야 합니다.")
        String imageUrl
) {
}
