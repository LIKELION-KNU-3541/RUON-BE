package com.springboot.ruon.domain.product.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 화장대 등록 요청 (POST /api/v1/products)
 * 남의 화장대에 등록할 수 있어, 인증 토큰에서 꺼내 쓴다.
 */
public record ProductCreateRequest(
        Long scanId,
        @NotBlank String productName,
        @NotBlank String brandName,
        @NotBlank String capacity
) {
}
