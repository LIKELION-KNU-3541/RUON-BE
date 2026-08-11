package com.springboot.ruon.domain.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 화장대 등록 요청 (POST /api/v1/products)
 *
 * userId는 임시로 요청 바디에 포함 - 인증(JWT) 구현되면
 * SecurityContext에서 꺼내는 방식으로 교체 예정.
 */
public record ProductCreateRequest(
        @NotNull Long userId,
        Long scanId,
        @NotBlank String productName,
        @NotBlank String brandName,
        @NotBlank String capacity
) {
}
