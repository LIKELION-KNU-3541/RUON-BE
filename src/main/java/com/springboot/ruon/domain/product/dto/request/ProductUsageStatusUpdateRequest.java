package com.springboot.ruon.domain.product.dto.request;

import com.springboot.ruon.domain.product.entity.UsageStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 제품 사용 상태 변경 요청 (PATCH /api/v1/products/{productId})
 */
public record ProductUsageStatusUpdateRequest(
        @NotNull UsageStatus usageStatus) {
}
