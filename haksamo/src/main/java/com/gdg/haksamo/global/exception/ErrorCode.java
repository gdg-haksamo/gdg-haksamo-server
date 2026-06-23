package com.gdg.haksamo.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 도메인 전반에서 사용하는 에러 코드 모음.
 * (HTTP 상태 + 클라이언트용 코드 + 기본 메시지)
 */
@Getter
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류가 발생했습니다."),

    // 인증 / 인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A005", "리프레시 토큰이 없습니다. 다시 로그인해주세요."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "A006", "리프레시 토큰이 일치하지 않습니다. 다시 로그인해주세요."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A007", "접근 권한이 없습니다."),

    // 사용자
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "U001", "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "사용자를 찾을 수 없습니다."),

    // 이메일 인증 (회원가입 1단계)
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "U003", "이메일 인증이 완료되지 않았습니다."),
    VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "U004", "인증 요청 내역이 없습니다. 인증번호를 먼저 받아주세요."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "U005", "인증번호가 만료되었습니다. 다시 요청해주세요."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "U006", "인증번호가 일치하지 않습니다."),
    VERIFICATION_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "U007", "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}