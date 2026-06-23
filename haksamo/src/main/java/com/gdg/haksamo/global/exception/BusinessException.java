package com.gdg.haksamo.global.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 던지는 예외. {@link ErrorCode}로 응답이 결정된다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}