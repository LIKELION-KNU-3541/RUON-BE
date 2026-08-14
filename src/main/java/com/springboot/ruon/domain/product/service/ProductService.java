package com.springboot.ruon.domain.product.service;

import com.springboot.ruon.auth.data.entity.User;
import com.springboot.ruon.auth.data.repository.UserRepository;
import com.springboot.ruon.domain.product.dto.request.ProductCreateRequest;
import com.springboot.ruon.domain.product.dto.response.ProductResponse;
import com.springboot.ruon.domain.product.dto.response.ProductAnalysisSummaryResponse;
import com.springboot.ruon.domain.product.dto.response.ProductDetailResponse;
import com.springboot.ruon.domain.product.dto.response.ProductListResponse;
import com.springboot.ruon.domain.product.dto.response.ProductPageResponse;
import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.product.entity.UsageStatus;
import com.springboot.ruon.domain.product.dto.response.CategoryCount;
import com.springboot.ruon.domain.product.repository.ProductRepository;
import com.springboot.ruon.domain.rag.service.RagService;
import com.springboot.ruon.domain.routine.service.llm.RoutineLlmService;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.AnalysisCategory;
import com.springboot.ruon.domain.scan.entity.ScanJob;
import com.springboot.ruon.domain.scan.repository.ScanJobRepository;
import com.springboot.ruon.domain.scan.service.AnalysisSummaryService;
import com.springboot.ruon.domain.scan.service.llm.ProductInfoLlmResult;
import com.springboot.ruon.domain.scan.service.llm.ProductInfoLlmService;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import com.springboot.ruon.global.storage.service.ImageStorageService;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ScanJobRepository scanJobRepository;
    private final RagService ragService;
    private final AnalysisSummaryService analysisSummaryService;
    private final ProductInfoLlmService productInfoLlmService;
    private final UserRepository userRepository;
    private final RoutineLlmService routineLlmService;
    private final ImageStorageService imageStorageService;

    @Transactional
    public ProductResponse createProduct(Long userId, ProductCreateRequest request) {
        ProductFields fields = request.scanId() != null
                ? resolveFromScan(userId, request)
                : resolveFromRequest(request);

        Product product = Product.builder()
                .userId(userId)
                .scanId(request.scanId())
                .productName(fields.productName())
                .brandName(fields.brandName())
                .capacity(fields.capacity())
                .analysisCategory(fields.analysisCategory())
                .build();

        // 루틴 스텝에 표시할 한 줄 소개를 등록 시점에 바로 생성한다 (실패해도 등록 자체는 진행,
        // 이후 루틴 생성 시 description이 비어있으면 그때 다시 한번 생성을 시도함).
        generateDescriptionQuietly(product, userId);

        product.applyAnalysisDescription(
                resolveAnalysisDescription(product, fields.analysisReason()));

        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }
    //전체 화장품 목록 카드에 쓸 한줄 문구, 주의할게 없으면 제품의 베네핏 소개 사용,  그외 primaryCard.description에 있는 내용을 사용을 함.
    // 소개 생성이 실패해 비어있는 경우는 분석 사유로 대체
    private String resolveAnalysisDescription(Product product, String analysisReason) {
        if (product.getAnalysisCategory() == AnalysisCategory.KEEP_USING
                && !isBlank(product.getDescription())) {
            return product.getDescription();
        }
        return analysisReason;
    }

    private void generateDescriptionQuietly(Product product, Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            String description = routineLlmService.generateProductDescription(product, user);
            product.applyDescription(description);
        } catch (Exception e) {
            log.warn("제품 등록 시 한 줄 소개 생성에 실패했습니다. productName={}", product.getProductName(), e);
        }
    }

    // scanId가 있으면 스캔 결과(구조화된 값)로 채운다. 직접 입력한 값이 있으면 스캔 결과보다 우선한다.
    private ProductFields resolveFromScan(Long userId, ProductCreateRequest request) {
        ScanJob scanJob = scanJobRepository.findById(request.scanId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "스캔 작업을 찾을 수 없습니다.", null));

        if (!scanJob.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인의 스캔만 사용할 수 있습니다.", null);
        }

        ProductInfoLlmResult scanResult = productInfoLlmService.fromJson(scanJob.getStructuredResult());
        if (scanResult == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "아직 분석이 완료되지 않은 스캔입니다.", null);
        }

        String productName = orElse(request.productName(), scanResult.productName());
        String brandName = orElse(request.brandName(), scanResult.brandName());
        String capacity = orElse(request.capacity(), scanResult.capacity());

        // 목록의 카테고리 필터·정렬을 SQL로 처리하려면 컬럼에 있어야 함 JSON이 아니라 컬럼에 있어야함.
        AnalysisCategory analysisCategory = classify(scanJob);
        AnalysisSummaryResponse summary = analysisSummaryService.fromJson(scanJob.getAnalysisSummary());
        String analysisReason = summary != null && summary.primaryCard() != null
                ? summary.primaryCard().description()
                : null;

        return requireComplete(new ProductFields(
                productName, brandName, capacity, analysisCategory, analysisReason));
    }

    // scanId가 없으면 직접 입력한 값만 사용한다. 분석 결과가 없으므로 카테고리도 비워 둔다.
    private ProductFields resolveFromRequest(ProductCreateRequest request) {
        return requireComplete(new ProductFields(
                request.productName(), request.brandName(), request.capacity(), null, null));
    }

    private ProductFields requireComplete(ProductFields fields) {
        if (isBlank(fields.productName()) || isBlank(fields.brandName()) || isBlank(fields.capacity())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "제품명/브랜드명/용량을 확인할 수 없습니다.", null);
        }
        return fields;
    }

    private String orElse(String requestValue, String scanValue) {
        return !isBlank(requestValue) ? requestValue : scanValue;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ProductFields(String productName, String brandName, String capacity,
                                 AnalysisCategory analysisCategory, String analysisReason) {
    }

    public ProductDetailResponse getProduct(Long userId, Long productId) {
        Product product = findProductOrThrow(productId);
        requireOwner(product, userId);
        return buildDetailResponse(product);
    }

    // scanId가 있으면 스캔 결과(이미지·전성분·분석 카드)를 조회 시점에 함께 가져와 합친다.
    // (등록 시점에 복사해두지 않는 이유: 이미지 URL은 presigned라 만료되고, 전성분/분석 카드는
    //  이미 ScanJob에 저장돼 있어 Product에 또 복사하면 데이터가 두 곳에서 어긋날 수 있음)
    private ProductDetailResponse buildDetailResponse(Product product) {
        if (product.getScanId() == null) {
            return ProductDetailResponse.withoutScanData(product);
        }

        ScanJob scanJob = scanJobRepository.findById(product.getScanId()).orElse(null);
        if (scanJob == null) {
            return ProductDetailResponse.withoutScanData(product);
        }

        String imageUrl = imageStorageService.generateViewUrl(viewImageObjectKey(scanJob));

        ProductInfoLlmResult structured = productInfoLlmService.fromJson(scanJob.getStructuredResult());
        List<String> fullIngredients = structured != null && structured.fullIngredients() != null
                ? structured.fullIngredients()
                : List.of();

        //상세 분석 결과를 다시 계산하지 않고, 스캔 완료 시 미리 만들어둔 화면 요약을 그대로 재사용한다.
        AnalysisSummaryResponse summary = analysisSummaryService.fromJson(scanJob.getAnalysisSummary());

        return ProductDetailResponse.of(
                product,
                imageUrl,
                fullIngredients,
                summary != null ? summary.primaryCard() : null,
                summary != null ? summary.secondaryCard() : null);
    }

    //대표 이미지는 못 찾을 수 있다. 그때는 사용자가 올린 사진을 보여준다. (ScanService와 동일한 로직)
    private String viewImageObjectKey(ScanJob scanJob) {
        String representativeImageObjectKey = scanJob.getRepresentativeImageObjectKey();
        return representativeImageObjectKey != null
                ? representativeImageObjectKey
                : scanJob.getUploadedImageObjectKey();
    }

    /**
     * 화장대 목록. 카테고리를 넘기지 않으면 전체를 본다.
     * 이미지는 이 페이지에 보이는 제품의 스캔만 읽어서 만든다.
     */
    public ProductPageResponse getProducts(
            Long userId, UsageStatus usageStatus, AnalysisCategory category, Pageable pageable) {

        Page<Product> page = productRepository.findForList(userId, usageStatus, category, pageable);

        Map<Long, ScanJob> scansById = findScansOf(page.getContent(), userId);
        List<ProductListResponse> products = page.getContent().stream()
                .map(product -> ProductListResponse.of(product, imageUrlOf(product, scansById)))
                .toList();

        return ProductPageResponse.of(products, page);
    }

    private Map<Long, ScanJob> findScansOf(List<Product> products, Long userId) {
        List<Long> scanIds = products.stream()
                .map(Product::getScanId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (scanIds.isEmpty()) {
            return Map.of();
        }
        return scanJobRepository.findAllById(scanIds).stream()
                .filter(scan -> scan.getUserId().equals(userId))
                .collect(Collectors.toMap(ScanJob::getScanId, Function.identity()));
    }

    //스캔 없이 직접 등록했거나 스캔이 지워졌으면 보여줄 이미지가 없다.
    private String imageUrlOf(Product product, Map<Long, ScanJob> scansById) {
        if (product.getScanId() == null) {
            return null;
        }
        ScanJob scanJob = scansById.get(product.getScanId());
        return scanJob != null ? imageStorageService.generateViewUrl(viewImageObjectKey(scanJob)) : null;
    }

    public ProductAnalysisSummaryResponse getAnalysisSummary(Long userId) {
        Map<AnalysisCategory, Integer> counts = new EnumMap<>(AnalysisCategory.class);
        for (CategoryCount row : productRepository.countByAnalysisCategory(userId)) {
            //스캔 없이 직접 등록한 제품은 카테고리가 없다. 분류할 근거가 없으므로 추가 확인으로 본다.
            //원래 기획에서는 제품을 직접 등록 할 수 없게 되어있는 알고있는데 해당 코드를 만든 부분이 이미 있길래 일단 처리는 해두었습니다..
            AnalysisCategory category = row.category() != null
                    ? row.category()
                    : AnalysisCategory.NEEDS_REVIEW;
            counts.merge(category, (int) row.count(), Integer::sum);
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
