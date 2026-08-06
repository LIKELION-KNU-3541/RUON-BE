package com.springboot.ruon.global.exception.Image;

/** S3 이미지 업로드에 실패했을 때 발생한다. */
public class ImageUploadException extends ImageStorageException {

    public ImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
