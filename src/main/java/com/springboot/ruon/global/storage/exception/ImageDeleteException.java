package com.springboot.ruon.global.storage.exception;

/** S3 이미지 삭제에 실패했을 때 발생한다. */ //해당 패키지느 merge 후에 추가로 수정한다.
public class ImageDeleteException extends ImageStorageException {

    public ImageDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
