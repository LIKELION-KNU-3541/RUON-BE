package com.springboot.ruon.global.storage.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 스캔 이미지를 위한 내부 이미지 스토리지 추상화.
 - 인터페이스 분리 원칙
 */
public interface ImageStorageService {


     // 이미지를 검증한 뒤 업로드하고, 생성된 objectKey를 반환.
     //DB에는 objectKey로 저장
    String upload(Long userId, MultipartFile file);


    //카카오 API를 통해 가져온 이미지에서 바이트를 이미 들고 있는 경우
    String upload(Long userId, byte[] image, String contentType);

    /**
     * 내부 처리(예: OCR)를 위해 원본 이미지 바이트를 조회한다.
     */
    byte[] download(String objectKey);


    //이미지를 삭제한다. 존재하지 않는 objectKey 삭제는 no-op
    void delete(String objectKey);
}
