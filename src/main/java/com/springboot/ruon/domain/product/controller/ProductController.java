package com.springboot.ruon.domain.product.controller;

import com.springboot.ruon.auth.security.CustomUserDetails;
import com.springboot.ruon.domain.product.dto.request.ProductCreateRequest;
import com.springboot.ruon.domain.product.dto.request.ProductUsageStatusUpdateRequest;
import com.springboot.ruon.domain.product.dto.response.ProductResponse;
import com.springboot.ruon.domain.product.dto.response.ProductAnalysisSummaryResponse;
import com.springboot.ruon.domain.product.dto.response.ProductDetailResponse;
import com.springboot.ruon.domain.product.dto.response.ProductPageResponse;
import com.springboot.ruon.domain.product.entity.UsageStatus;
import com.springboot.ruon.domain.product.service.ProductService;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.AnalysisCategory;
import com.springboot.ruon.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    //제품 조회시 반환되는 제품 최대 개수 조절
    private static final int MAX_PAGE_SIZE = 10;

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

    /*
     * 화장대 목록. 기본은 사용중인 제품만 최근 등록순으로 본다.
     * category를 주면 그 분류만, usageStatus를 주면 사용중단한 제품도 볼 수 있다.
     * page는 1부터 시작한다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProductPageResponse>> getProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) AnalysisCategory category,
            @RequestParam(defaultValue = "IN_USE") UsageStatus usageStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page - 1, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "productId"));

        ProductPageResponse response = productService.getProducts(
                userDetails.getUserId(), usageStatus, category, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
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
