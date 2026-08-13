package com.springboot.ruon.domain.product.entity;

import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.AnalysisCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 화장대에 등록된 화장품 (ERD의 PRODUCT 테이블)
 * userId / scanId는 User, ScanJob과 직접 연관관계를 맺지 않고 Long으로만 둠.
 */
@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "scan_id")
    private Long scanId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "brand_name", nullable = false)
    private String brandName;

    //원문 표기를 그대로 보관한다. (예: 50ml, 1.7 fl.oz)
    @Column(name = "capacity", nullable = false)
    private String capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_status", nullable = false)
    private UsageStatus usageStatus;

    // LLM이 생성한 한 줄 소개 문구. 제품당 한 번만 생성해서 재사용
    @Lob
    @Column(name = "description")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_category")
    private AnalysisCategory analysisCategory;

    //목록 카드에 보여줄 한 줄 문구. 루틴용 description과 용도가 다름.
    @Column(name = "analysis_description", length = 500)
    private String analysisDescription;

    @Builder
    public Product(Long scanId, Long userId, String productName, String brandName,
                   String capacity, UsageStatus usageStatus,
                   AnalysisCategory analysisCategory) {
        this.scanId = scanId;
        this.userId = userId;
        this.productName = productName;
        this.brandName = brandName;
        this.capacity = capacity;
        this.usageStatus = usageStatus != null ? usageStatus : UsageStatus.IN_USE;
        this.analysisCategory = analysisCategory;
    }

    public void changeUsageStatus(UsageStatus usageStatus) {
        this.usageStatus = usageStatus;
    }

    public void applyDescription(String description) {
        this.description = description;
    }

    public void applyAnalysisDescription(String analysisDescription) {
        this.analysisDescription = analysisDescription;
    }
}
