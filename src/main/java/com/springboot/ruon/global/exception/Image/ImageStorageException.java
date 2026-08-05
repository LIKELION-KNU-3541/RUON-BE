package com.springboot.ruon.global.exception.Image;

/**
 * S3 스토리지 모듈의 최상위 예외.
 * <p>
 * 하위 예외는 GlobalExceptionHandler에서 팀 공통 ErrorCode로 변환된다.
 */
public class ImageStorageException extends RuntimeException {

    public ImageStorageException(String message) {
        super(message);
    }

    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
