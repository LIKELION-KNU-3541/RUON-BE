package com.springboot.ruon.domain.product.dto.request;

/**
 * 화장대 등록 요청 (POST /api/v1/products)
 * 남의 화장대에 등록할 수 있어, 인증 토큰에서 꺼내 쓴다.
 *
 * scanId가 있으면 스캔 결과(구조화된 브랜드/제품명/용량)로 자동 채워지므로
 * productName/brandName/capacity는 선택값이다. scanId가 없으면 직접 입력한 값을 그대로 사용한다.
 */
public record ProductCreateRequest(
        Long scanId,
        String productName,
        String brandName,
        String capacity
) {
}
