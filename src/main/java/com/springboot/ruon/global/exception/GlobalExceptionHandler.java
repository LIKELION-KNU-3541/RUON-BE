package com.springboot.ruon.global.exception;

import com.springboot.ruon.global.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

    private String responseMessage(CustomException e, ErrorCode errorCode) {
        if (errorCode.getStatus().is4xxClientError() && e.getMessage() != null) {
            return e.getMessage();
        }
        return errorCode.getMessage();
    }

    //400처리
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        log.warn("잘못된 요청: {}", e.getClass().getSimpleName());
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST.name(), ErrorCode.INVALID_REQUEST.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("업로드 크기 초과", e);
        return ResponseEntity
                .status(ErrorCode.INVALID_IMAGE.getStatus())
                .body(ApiResponse.fail(ErrorCode.INVALID_IMAGE.name(), "이미지 크기가 허용 범위를 초과했습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}