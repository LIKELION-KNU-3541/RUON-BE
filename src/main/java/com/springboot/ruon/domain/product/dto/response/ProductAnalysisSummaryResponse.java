package com.springboot.ruon.domain.product.dto.response;

import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.AnalysisCategory;
import java.util.List;
import java.util.Map;

/** 사용자의 화장대에 등록된 제품을 RAG 최종 분류별로 집계한 응답. */
public record ProductAnalysisSummaryResponse(
        int totalCount,
        List<AnalysisCount> analysisCards) {

    public static ProductAnalysisSummaryResponse from(Map<AnalysisCategory, Integer> counts) {
        List<AnalysisCount> cards = List.of(
                card(AnalysisCategory.KEEP_USING, "사용 유지", counts),
                card(AnalysisCategory.PAUSE, "잠시 보류", counts),
                card(AnalysisCategory.SELECTIVE_USE, "선택 사용", counts),
                card(AnalysisCategory.NEEDS_REVIEW, "추가 확인", counts));
        int totalCount = cards.stream().mapToInt(AnalysisCount::count).sum();
        return new ProductAnalysisSummaryResponse(totalCount, cards);
    }

    private static AnalysisCount card(
            AnalysisCategory category, String title, Map<AnalysisCategory, Integer> counts) {
        return new AnalysisCount(category, title, counts.getOrDefault(category, 0));
    }

    public record AnalysisCount(AnalysisCategory category, String title, int count) {
    }
}
