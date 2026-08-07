package com.springboot.ruon.global.exception;

import com.springboot.ruon.global.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("CustomException: {}", errorCode.name(), e);
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.name(), responseMessage(e, errorCode)));
    }

    /**
     * 4xx는 사용자가 조치할 수 있는 정보이므로 상세 사유를 전달하고,
     * 5xx는 objectKey나 외부 API 메시지가 새어나가지 않도록 ErrorCode 기본 메시지만 내보낸다.
     */
    private String responseMessage(CustomException e, ErrorCode errorCode) {
        if (errorCode.getStatus().is4xxClientError() && e.getMessage() != null) {
            return e.getMessage();
        }
        return errorCode.getMessage();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
