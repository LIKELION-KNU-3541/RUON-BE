package com.springboot.ruon.global.exception.Image;

import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;

/**
 * 이미지 스토리지 처리 중 발생하는 예외.*/
public class ImageStorageException extends CustomException {

    public ImageStorageException(ErrorCode errorCode, String detail) {
        this(errorCode, detail, null);
    }

    public ImageStorageException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }
}
