package com.springboot.ruon.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail != null ? detail : errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
