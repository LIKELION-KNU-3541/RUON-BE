package com.springboot.ruon.global.ocr.dto;

import java.util.List;

/**
 * 이미지를 GCS URI가 아니라 base64 인라인으로 보냄
 */
public record VisionAnnotateRequest(List<AnnotateImageRequest> requests) {

    private static final String DOCUMENT_TEXT_DETECTION = "DOCUMENT_TEXT_DETECTION";

    //성분표처럼 밀집된 인쇄 텍스트에는 DOCUMENT_TEXT_DETECTION이 적합
    public static VisionAnnotateRequest documentTextDetection(String base64Image) {
        return new VisionAnnotateRequest(List.of(
                new AnnotateImageRequest(
                        new Image(base64Image),
                        List.of(new Feature(DOCUMENT_TEXT_DETECTION)))));
    }

    public record AnnotateImageRequest(Image image, List<Feature> features) {
    }

    public record Image(String content) {
    }

    public record Feature(String type) {
    }
}
