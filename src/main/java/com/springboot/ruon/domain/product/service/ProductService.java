package com.springboot.ruon.domain.product.service;

import com.springboot.ruon.domain.product.dto.request.ProductCreateRequest;
import com.springboot.ruon.domain.product.dto.response.ProductResponse;
import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.product.entity.UsageStatus;
import com.springboot.ruon.domain.product.repository.ProductRepository;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

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
