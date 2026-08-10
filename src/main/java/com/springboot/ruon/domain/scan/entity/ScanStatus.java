package com.springboot.ruon.domain.scan.entity;


//OCR 스캔 작업 상태.
//GET Scan 비동기처리로 인한 상태 변경
public enum ScanStatus {

    //이미지 업로드 완료, OCR 대기
    UPLOADED,

    //OCR 진행중
    OCR_PROCESSING,

    //OCR 성공, 원문을 제품 정보로 구조화하는 중
    STRUCTURING,

    //구조화 성공, 구조화 결과로 대표 이미지를 찾는 중
    IMAGE_SEARCHING,

    //모든 처리 완료. 대표 이미지를 못 찾아도 이 상태가 된다.
    COMPLETED,

    // 필수 단계 실패
    FAILED
}
