package com.springboot.ruon.global.exception.Image;

/** 키 부재 이외의 이유로 S3 이미지 조회에 실패했을 때 발생한다. */
public class ImageDownloadException extends ImageStorageException {

    public ImageDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
