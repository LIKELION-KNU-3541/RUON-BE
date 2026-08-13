package com.springboot.ruon.domain.product.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 목록 응답. 화면 하단 페이지 표시(1 / 3)에 필요한 값을 함께 내려준다.
 * page는 1부터 시작한다.
 */
public record ProductPageResponse(
        List<ProductListResponse> products,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static ProductPageResponse of(List<ProductListResponse> products, Page<?> page) {
        return new ProductPageResponse(
                products,
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
