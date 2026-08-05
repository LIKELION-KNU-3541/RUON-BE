package com.springboot.ruon.global.exception;

import com.springboot.ruon.global.exception.Image.ImageNotFoundException;
import com.springboot.ruon.global.exception.Image.ImageStorageException;
import com.springboot.ruon.global.exception.Image.ImageValidationException;
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
                .body(ApiResponse.fail(errorCode.name(), errorCode.getMessage()));
    }

    // 이미지 스토리지

    @ExceptionHandler(ImageValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleImageValidation(ImageValidationException e) {
        log.warn("이미지 검증 실패", e);
        return ResponseEntity
                .status(ErrorCode.INVALID_IMAGE.getStatus())
                .body(ApiResponse.fail(ErrorCode.INVALID_IMAGE.name(), e.getMessage()));
    }
    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleImageNotFound(ImageNotFoundException e) {
        log.warn("이미지 찾을 수 없음", e);
        return ResponseEntity
                .status(ErrorCode.IMAGE_NOT_FOUND.getStatus())
                .body(ApiResponse.fail(ErrorCode.IMAGE_NOT_FOUND.name(), ErrorCode.IMAGE_NOT_FOUND.getMessage()));
    }

    /** 업로드·조회·삭제 실패. objectKey와 SDK 메시지는 로그에만 남긴다. */
    @ExceptionHandler(ImageStorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleImageStorage(ImageStorageException e) {
        log.error("S3 스토리지 오류", e);
        return ResponseEntity
                .status(ErrorCode.IMAGE_STORAGE_FAILED.getStatus())
                .body(ApiResponse.fail(ErrorCode.IMAGE_STORAGE_FAILED.name(), ErrorCode.IMAGE_STORAGE_FAILED.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
