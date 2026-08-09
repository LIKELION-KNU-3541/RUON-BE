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
    ROUTINE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 루틴을 찾을 수 없습니다."),
    STEP_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 단계를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다."),
    LLM_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "루틴 생성(LLM 호출)에 실패했습니다."),
    OCR_EXTRACTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "성분표 인식 실패"),
    RAG_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "성분 분석 서비스 응답 오류"),
    RAG_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "성분 분석 서비스를 사용할 수 없습니다."),
    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "올바르지 않은 이미지입니다."),
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
