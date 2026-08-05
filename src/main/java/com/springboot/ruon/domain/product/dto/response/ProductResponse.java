package com.springboot.ruon.domain.product.dto.response;

import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.product.entity.UsageStatus;

public record ProductResponse(
        Long productId,
        String productName,
        String category,
        UsageStatus usageStatus
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getProductId(),
                product.getProductName(),
                product.getCategory(),
                product.getUsageStatus()
        );
    }
}
