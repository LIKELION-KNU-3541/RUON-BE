package com.springboot.ruon.global.exception.Ocr;

//vision api 호출 실패
public class OcrRequestFailedException extends OcrException {

    public OcrRequestFailedException(String message) {
        super(message);
    }

    public OcrRequestFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
