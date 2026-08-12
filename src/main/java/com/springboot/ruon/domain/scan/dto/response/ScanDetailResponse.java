package com.springboot.ruon.domain.scan.dto.response;

import com.springboot.ruon.domain.scan.entity.ScanJob;
import com.springboot.ruon.domain.scan.entity.ScanFailureStage;
import com.springboot.ruon.domain.scan.entity.ScanStatus;
import com.springboot.ruon.domain.scan.service.llm.ProductInfoLlmResult;

//스캔 상태 및 제품 기본 정보 조회 응답. 분석 결과는 별도 API에서 반환한다.
public record ScanDetailResponse(
        Long scanId,
        ScanStatus status,
        ProductInfoLlmResult product,
        ScanFailureStage failureStage,
        String failureCode,
        String imageUrl) {

    //구조화 전에는 null, 카카오 API에서 가져온 이미지가 없으면 올린 이미지의 URL 반환
    public static ScanDetailResponse from(
            ScanJob scanJob,
            ProductInfoLlmResult product,
            String imageUrl) {
        return new ScanDetailResponse(
                scanJob.getScanId(),
                scanJob.getStatus(),
                product,
                scanJob.getFailureStage(),
                scanJob.getFailureCode(),
                imageUrl);
    }
}
