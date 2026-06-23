package com.gdg.haksamo.global.exception;

import com.gdg.haksamo.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 컨트롤러 계층에서 발생하는 예외를 공통 {@link ApiResponse} 형식으로 변환한다.
 * (시큐리티 필터 단계의 인증 실패는 SecurityConfig의 EntryPoint/AccessDeniedHandler가 처리)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: {} - {}", errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage()
                : ErrorCode.INVALID_INPUT_VALUE.getMessage();
        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE.getCode(), message));
    }

    /**
     * 잘못된 클라이언트 입력(깨진 JSON, 타입 불일치 enum/path 변수, 필수 파라미터 누락 등)은
     * 서버 오류가 아니라 400으로 분류한다. (아래 일반 Exception 핸들러에 걸려 500이 되는 것 방지)
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        log.warn("Bad request: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    /**
     * 동시 요청 등으로 DB 제약(유니크 등)이 깨진 경우 500이 아닌 409로 변환한다.
     * (예: 회원가입/관리자 계정 발급이 거의 동시에 들어와 이메일 유니크 충돌)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("DataIntegrityViolation: {}", e.getMostSpecificCause().getMessage());
        ErrorCode errorCode = ErrorCode.DATA_CONFLICT;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    /**
     * 메서드 보안(@PreAuthorize 등)으로 권한이 거부된 경우 403으로 변환.
     * (경로 기반 인가는 ExceptionTranslationFilter→JwtAccessDeniedHandler가 처리하지만,
     *  컨트롤러 메서드 단계에서 던져진 거부 예외는 DispatcherServlet이 먼저 잡으므로 여기서 처리해야
     *  아래 일반 Exception 핸들러에 걸려 500이 나가는 것을 막는다.)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        log.warn("AccessDenied: {}", e.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
}