package com.springboot.ruon.domain.scan.dto.response;

import com.springboot.ruon.domain.scan.entity.ScanJob;
import com.springboot.ruon.domain.scan.entity.ScanFailureStage;
import com.springboot.ruon.domain.scan.entity.ScanStatus;
import com.springboot.ruon.domain.scan.service.llm.ProductInfoLlmResult;
import com.springboot.ruon.domain.rag.dto.response.IngredientAnalysisResponse;

//스캔 결과 조회 응답 (GET /api/v1/scan/{scanId}) Dto.
public record ScanDetailResponse(
        Long scanId,
        ScanStatus status,
        ProductInfoLlmResult structuredResult,
        IngredientAnalysisResponse ingredientAnalysisResult,
        ScanFailureStage failureStage,
        String failureCode,
        String imageUrl) {

    //구조화 전에는 null, 카카오 API에서 가져온 이미지가 없으면 올린 이미지의 URL 반환
    public static ScanDetailResponse from(
            ScanJob scanJob,
            ProductInfoLlmResult structuredResult,
            IngredientAnalysisResponse ingredientAnalysisResult,
            String imageUrl) {
        return new ScanDetailResponse(
                scanJob.getScanId(),
                scanJob.getStatus(),
                structuredResult,
                ingredientAnalysisResult,
                scanJob.getFailureStage(),
                scanJob.getFailureCode(),
                imageUrl);
    }
}
