package com.springboot.ruon.domain.scan.entity;


//OCR 스캔 작업 상태.
//GET Scan 비동기처리로 인한 상태 변경
public enum ScanStatus {

    //이미지 업로드 완료, OCR 대기
    UPLOADED,

    //OCR 진행중
    OCR_PROCESSING,

    //OCR 성공, 원문 저장 완료
    COMPLETED,

    // 필수 단계 실패
    FAILED
}
