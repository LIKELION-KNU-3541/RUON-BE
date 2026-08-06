package com.springboot.ruon.global.exception.Image;

/** S3 이미지 삭제에 실패했을 때 발생한다. */
public class ImageDeleteException extends ImageStorageException {

    public ImageDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
