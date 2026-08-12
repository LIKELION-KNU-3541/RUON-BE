package com.springboot.ruon.domain.product.service;

import com.springboot.ruon.domain.product.dto.request.ProductCreateRequest;
import com.springboot.ruon.domain.product.dto.response.ProductResponse;
import com.springboot.ruon.domain.product.dto.response.ProductAnalysisSummaryResponse;
import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.product.entity.UsageStatus;
import com.springboot.ruon.domain.product.repository.ProductRepository;
import com.springboot.ruon.domain.rag.service.RagService;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.AnalysisCategory;
import com.springboot.ruon.domain.scan.entity.ScanJob;
import com.springboot.ruon.domain.scan.repository.ScanJobRepository;
import com.springboot.ruon.domain.scan.service.AnalysisSummaryService;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ScanJobRepository scanJobRepository;
    private final RagService ragService;
    private final AnalysisSummaryService analysisSummaryService;

    @Transactional
    public ProductResponse createProduct(Long userId, ProductCreateRequest request) {
        Product product = Product.builder()
                .userId(userId)
                .scanId(request.scanId())
                .productName(request.productName())
                .brandName(request.brandName())
                .capacity(request.capacity())
                .build();

        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }

    public ProductResponse getProduct(Long userId, Long productId) {
        Product product = findProductOrThrow(productId);
        requireOwner(product, userId);
        return ProductResponse.from(product);
    }

    public ProductAnalysisSummaryResponse getAnalysisSummary(Long userId) {
        var products = productRepository.findByUserId(userId);
        var scanIds = products.stream()
                .map(Product::getScanId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ScanJob> scansById = scanJobRepository.findAllById(scanIds).stream()
                .filter(scan -> scan.getUserId().equals(userId))
                .collect(Collectors.toMap(ScanJob::getScanId, Function.identity()));
        Map<AnalysisCategory, Integer> counts = new EnumMap<>(AnalysisCategory.class);

        for (Product product : products) {
            AnalysisCategory category = classify(scansById.get(product.getScanId()));
            counts.merge(category, 1, Integer::sum);
        }
        return ProductAnalysisSummaryResponse.from(counts);
    }

    private AnalysisCategory classify(ScanJob scanJob) {
        if (scanJob == null
                || scanJob.getIngredientAnalysisResult() == null
                || scanJob.getIngredientAnalysisResult().isBlank()) {
            return AnalysisCategory.NEEDS_REVIEW;
        }
        return analysisSummaryService
                .create(ragService.fromJson(scanJob.getIngredientAnalysisResult()))
                .category();
    }

    //제품이 없거나(404) 다른 사용자의 제품인 경우(403)
    @Transactional
    public ProductResponse updateUsageStatus(Long userId, Long productId, UsageStatus usageStatus) {
        Product product = findProductOrThrow(productId);
        requireOwner(product, userId);
        product.changeUsageStatus(usageStatus);
        return ProductResponse.from(product);
    }

    @Transactional
    public void deleteProduct(Long userId, Long productId) {
        Product product = findProductOrThrow(productId);
        requireOwner(product, userId);
        productRepository.delete(product);
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }
    private void requireOwner(Product product, Long userId) {
        if (!product.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인의 제품만 접근할 수 있습니다.", null);
        }
    }
}
