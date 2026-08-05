package com.springboot.ruon.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 화장대에 등록된 화장품 (ERD의 PRODUCT 테이블)
 *
 * userId / scanId는 User, ScanJob 엔티티가 아직 없어서
 * * 우선 Long 컬럼으로만 두고, 추후 엔티티가 준비되면 @ManyToOne 연관관계로 교체 예정.
 *
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

    @Column(name = "category")
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_status", nullable = false)
    private UsageStatus usageStatus;

    @Builder
    public Product(Long scanId, Long userId, String productName, String category,
                    UsageStatus usageStatus) {
        this.scanId = scanId;
        this.userId = userId;
        this.productName = productName;
        this.category = category;
        this.usageStatus = usageStatus != null ? usageStatus : UsageStatus.IN_USE;
    }

    public void changeUsageStatus(UsageStatus usageStatus) {
        this.usageStatus = usageStatus;
    }
}
