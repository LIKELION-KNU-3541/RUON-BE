package com.springboot.ruon.global.storage.exception;

/**
 * S3 스토리지 모듈의 최상위 예외.
 * <p>
 * 모듈 내부 임시 예외: 팀 공통 CustomException / ErrorCode 구조가
 * develop에 병합되면 하위 예외들을 공통 구조로 매핑하거나 교체해야 한다.
 */
public class ImageStorageException extends RuntimeException {

    public ImageStorageException(String message) {
        super(message);
    }

    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
