package com.springboot.ruon.global.exception.Ocr;

/** Vision 호출은 성공했지만 이미지에서 텍스트 찾지 못했을 때 발생 */
public class OcrExtractionFailedException extends OcrException {

    public OcrExtractionFailedException(String message) {
        super(message);
    }
}
