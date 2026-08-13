package com.springboot.ruon.domain.product.dto.response;

import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.AnalysisCategory;

/**
 * 카테고리별 제품 수 집계 결과.
 */
public record CategoryCount(AnalysisCategory category, long count) {
}
