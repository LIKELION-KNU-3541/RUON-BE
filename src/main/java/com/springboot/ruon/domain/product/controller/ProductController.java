package com.springboot.ruon.domain.product.controller;

import com.springboot.ruon.auth.security.CustomUserDetails;
import com.springboot.ruon.domain.product.dto.request.ProductCreateRequest;
import com.springboot.ruon.domain.product.dto.request.ProductUsageStatusUpdateRequest;
import com.springboot.ruon.domain.product.dto.response.ProductResponse;
import com.springboot.ruon.domain.product.dto.response.ProductAnalysisSummaryResponse;
import com.springboot.ruon.domain.product.dto.response.ProductDetailResponse;
import com.springboot.ruon.domain.product.service.ProductService;
import com.springboot.ruon.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 화장대 등록
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = productService.createProduct(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // 화장대에 등록된 화장품의 RAG 분류별 개수 조회
    @GetMapping("/analysis-summary")
    public ResponseEntity<ApiResponse<ProductAnalysisSummaryResponse>> getAnalysisSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success(productService.getAnalysisSummary(userDetails.getUserId())));
    }

    // 화장품 단일 제품 조회 (scanId가 있으면 이미지·전성분·분석 카드까지 함께 반환)
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId) {
        ProductDetailResponse response = productService.getProduct(userDetails.getUserId(), productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 화장품 사용 상태 변경
    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateUsageStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody ProductUsageStatusUpdateRequest request) {

        ProductResponse response = productService.updateUsageStatus(
                userDetails.getUserId(), productId, request.usageStatus());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 화장품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId) {
        productService.deleteProduct(userDetails.getUserId(), productId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
