package com.springboot.ruon.global.exception.Ocr;

import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;


public class OcrException extends CustomException {

    public OcrException(ErrorCode errorCode, String detail) {
        this(errorCode, detail, null);
    }

    public OcrException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }
}
