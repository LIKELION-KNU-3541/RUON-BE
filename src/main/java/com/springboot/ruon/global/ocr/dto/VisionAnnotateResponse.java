package com.springboot.ruon.global.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 필요한 것만 걸러서 받음.
 * Vision은 개별 이미지 처리 실패를 HTTP 오류가 아니라 응답 안의 {@code error}로 돌려주므로
 * 그 필드도 함께 읽는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VisionAnnotateResponse(List<AnnotateImageResponse> responses) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AnnotateImageResponse(FullTextAnnotation fullTextAnnotation, VisionError error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FullTextAnnotation(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VisionError(Integer code, String message) {
    }
}
