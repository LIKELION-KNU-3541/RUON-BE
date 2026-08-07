package com.springboot.ruon.global.exception;

import com.springboot.ruon.global.exception.Image.ImageNotFoundException;
import com.springboot.ruon.global.exception.Image.ImageStorageException;
import com.springboot.ruon.global.exception.Image.ImageValidationException;
import com.springboot.ruon.global.exception.Ocr.OcrException;
import com.springboot.ruon.global.exception.Ocr.OcrExtractionFailedException;
import com.springboot.ruon.global.exception.Ocr.OcrImageTooLargeException;
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

    // 이미지 OCR

    /** 이미지에서 텍스트를 찾지 못한 경우. 서버 오류가 아니라 처리 결과이므로 422로 구분한다. */
    @ExceptionHandler(OcrExtractionFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleOcrExtractionFailed(OcrExtractionFailedException e) {
        log.warn("OCR 텍스트 추출 실패: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.OCR_EXTRACTION_FAILED.getStatus())
                .body(ApiResponse.fail(ErrorCode.OCR_EXTRACTION_FAILED.name(), ErrorCode.OCR_EXTRACTION_FAILED.getMessage()));
    }

    /** base64 변환 후 Vision 요청 크기 제한을 넘은 경우. 사용자가 조치할 수 있으므로 사유를 전달한다. */
    @ExceptionHandler(OcrImageTooLargeException.class)
    public ResponseEntity<ApiResponse<Void>> handleOcrImageTooLarge(OcrImageTooLargeException e) {
        log.warn("OCR 이미지 크기 초과: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_IMAGE.getStatus())
                .body(ApiResponse.fail(ErrorCode.INVALID_IMAGE.name(), "이미지가 너무 커서 인식할 수 없습니다."));
    }

    /** Vision 호출 실패 등 그 외 OCR 오류. 외부 API 상세는 로그에만 남긴다. */
    @ExceptionHandler(OcrException.class)
    public ResponseEntity<ApiResponse<Void>> handleOcr(OcrException e) {
        log.error("OCR 처리 오류", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
