package com.springboot.ruon.domain.scan.dto.response;

import com.springboot.ruon.domain.scan.entity.ScanJob;
import com.springboot.ruon.domain.scan.entity.ScanStatus;
import com.springboot.ruon.domain.scan.service.llm.ProductInfoLlmResult;

//스캔 결과 조회 응답 (GET /api/v1/scan/{scanId}) Dto.
public record ScanDetailResponse(
        Long scanId,
        ScanStatus status,
        ProductInfoLlmResult structuredResult,
        String imageUrl) {

    //구조화 전에는 null, 카카오 API에서 가져온 이미지가 없으면 올린 이미지의 URL 반환
    public static ScanDetailResponse from(
            ScanJob scanJob, ProductInfoLlmResult structuredResult, String imageUrl) {
        return new ScanDetailResponse(
                scanJob.getScanId(), scanJob.getStatus(), structuredResult, imageUrl);
    }
}
