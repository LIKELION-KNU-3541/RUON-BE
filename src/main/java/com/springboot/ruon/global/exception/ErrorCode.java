package com.springboot.ruon.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 팀 공통 에러 코드
 */
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 파라미터 오류"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증 실패/토큰 만료"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "타인의 리소스 접근"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스 없음"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 제품을 찾을 수 없습니다."),
    OCR_EXTRACTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "성분표 인식 실패"),
    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "올바르지 않은 이미지입니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다."),
    IMAGE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장소 처리에 실패했습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청 한도 초과"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
