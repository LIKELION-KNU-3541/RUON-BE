package com.springboot.ruon.global.storage.exception;

/** 업로드 파일이 이미지 검증(빈 파일, 크기 초과, 형식 불일치)에 실패했을 때 발생한다. */
public class ImageValidationException extends ImageStorageException {

    public ImageValidationException(String message) {
        super(message);
    }
}
