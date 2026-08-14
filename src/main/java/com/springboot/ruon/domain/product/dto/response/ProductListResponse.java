package com.springboot.ruon.domain.product.dto.response;

import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.product.entity.UsageStatus;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.AnalysisCategory;

/**
 * 화장대 목록의 카드 하나.
 * 상세 화면과 달리 카드에 필요한 것만 담는다.
 */
public record ProductListResponse(
        Long productId,
        String imageUrl,
        String brandName,
        String productName,
        String description,
        AnalysisCategory analysisCategory,
        UsageStatus usageStatus) {

    public static ProductListResponse of(Product product, String imageUrl) {
        return new ProductListResponse(
                product.getProductId(),
                imageUrl,
                product.getBrandName(),
                product.getProductName(),
                product.getAnalysisDescription(),
                product.getAnalysisCategory(),
                product.getUsageStatus());
    }
}
