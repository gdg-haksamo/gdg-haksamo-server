package com.gdg.haksamo.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 모든 API 응답을 감싸는 공통 래퍼.
 * 성공: {"success": true, "data": ...}
 * 실패: {"success": false, "code": "U001", "message": "..."}
 * (null 필드는 직렬화에서 제외)
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, null, data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
