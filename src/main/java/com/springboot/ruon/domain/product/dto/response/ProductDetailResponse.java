package com.springboot.ruon.domain.product.dto.response;

import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.product.entity.UsageStatus;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.Card;
import java.util.List;

/**
 * 화장품 단일 제품 조회 응답 (GET /api/v1/products/{productId}).
 *
 * scanId가 있는 제품은 스캔 결과(이미지·전성분·분석 카드)를 함께 보여준다.
 * scanId가 없거나(직접 등록) 스캔 데이터를 찾을 수 없으면 imageUrl/카드는 null,
 * fullIngredients는 빈 배열로 내려간다.
 */
public record ProductDetailResponse(
        Long productId,
        String productName,
        String brandName,
        String capacity,
        UsageStatus usageStatus,
        String description,
        String imageUrl,
        List<String> fullIngredients,
        Card primaryCard,
        Card secondaryCard
) {
    public static ProductDetailResponse of(
            Product product,
            String imageUrl,
            List<String> fullIngredients,
            Card primaryCard,
            Card secondaryCard) {
        return new ProductDetailResponse(
                product.getProductId(),
                product.getProductName(),
                product.getBrandName(),
                product.getCapacity(),
                product.getUsageStatus(),
                product.getDescription(),
                imageUrl,
                fullIngredients,
                primaryCard,
                secondaryCard);
    }

    //scanId가 없거나 스캔 데이터를 찾을 수 없는 경우
    public static ProductDetailResponse withoutScanData(Product product) {
        return of(product, null, List.of(), null, null);
    }
}
