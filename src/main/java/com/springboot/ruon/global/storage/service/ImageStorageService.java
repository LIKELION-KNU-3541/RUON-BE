package com.springboot.ruon.global.storage.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 스캔 이미지를 위한 내부 이미지 스토리지 추상화.
 - 인터페이스 분리 원칙
 */
public interface ImageStorageService {


     // 이미지를 검증한 뒤 업로드하고, 생성된 objectKey를 반환한다.
     //DB에는 URL이 아닌  objectKey를 저장
    String upload(Long userId, MultipartFile file);

    /**
     * 내부 처리(예: OCR)를 위해 원본 이미지 바이트를 조회한다.
     */
    byte[] download(String objectKey);

    /**
     * 이미지를 삭제한다. 존재하지 않는 objectKey 삭제는 no-op이며
     * (S3 삭제는 멱등), 정리 경로에서 안전하게 호출할 수 있다.
     */
    void delete(String objectKey);
}
