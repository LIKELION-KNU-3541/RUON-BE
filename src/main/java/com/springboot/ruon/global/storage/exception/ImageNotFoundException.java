package com.springboot.ruon.global.storage.exception;

/** 요청한 objectKey가 버킷에 존재하지 않을 때 발생한다. */
public class ImageNotFoundException extends ImageStorageException {

    public ImageNotFoundException(String objectKey, Throwable cause) {
        super("이미지를 찾을 수 없습니다: " + objectKey, cause);
    }
}
