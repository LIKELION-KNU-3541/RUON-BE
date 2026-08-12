package com.springboot.ruon.domain.scan.dto.response;

import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.Card;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.CautionIngredient;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.OverallStatus;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.Summary;
import java.util.List;

/** 제품 확인 화면에서 사용하는 RAG 분석 요약 응답. */
public record ScanAnalysisResponse(
        Long scanId,
        OverallStatus overallStatus,
        Card primaryCard,
        Card secondaryCard,
        Summary summary,
        List<CautionIngredient> cautionIngredients,
        List<String> unknownIngredients) {

    public static ScanAnalysisResponse from(Long scanId, AnalysisSummaryResponse analysis) {
        return new ScanAnalysisResponse(
                scanId,
                analysis.overallStatus(),
                analysis.primaryCard(),
                analysis.secondaryCard(),
                analysis.summary(),
                analysis.cautionIngredients(),
                analysis.unknownIngredients());
    }
}
