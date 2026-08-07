package com.springboot.ruon.global.ocr.service;

public interface OcrService {

    /**
     * @param image 원본 이미지 바이트 (스토리지에서 조회한 값)
     * @return 추출된 원문 텍스트
     */
    String extractText(byte[] image);
}
