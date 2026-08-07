package com.springboot.ruon.global.exception.Ocr;

/**
 * base64로 변환한 이미지가 Vision의 요청 크기 제한을 넘었을 때 발생한다.
 * <p>
 * 업로드 제한(7MB)은 base64 증가분(약 37%)을 감안한 값이지만 여유가 크지 않아,
 * 경계에 걸리는 경우 Vision의 모호한 오류 대신 원인이 분명한 예외를 던진다.
 */
public class OcrImageTooLargeException extends OcrException {

    public OcrImageTooLargeException(String message) {
        super(message);
    }
}
