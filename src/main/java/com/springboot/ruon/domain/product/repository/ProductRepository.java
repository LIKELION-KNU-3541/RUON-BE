package com.springboot.ruon.domain.product.repository;

import com.springboot.ruon.domain.product.dto.response.CategoryCount;
import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.product.entity.UsageStatus;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.AnalysisCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 루틴 생성 시 유저가 화장대에 등록해둔 제품 목록을 읽어오기 위해 사용
    List<Product> findByUserId(Long userId);
    //화장대 목록, 카테고리를 넘기지 않으면 전체 조회
    @Query("""
            select p from Product p
            where p.userId = :userId
              and p.usageStatus = :usageStatus
              and (:category is null or p.analysisCategory = :category)
            """)
    Page<Product> findForList(@Param("userId") Long userId,
                              @Param("usageStatus") UsageStatus usageStatus,
                              @Param("category") AnalysisCategory category,
                              Pageable pageable);

    // 카테고리는 등록 시점에 컬럼으로 확정되므로 집계를 DB에서 처리한다.
    @Query("""
            select new com.springboot.ruon.domain.product.dto.response.CategoryCount(
                    p.analysisCategory, count(p))
            from Product p
            where p.userId = :userId
            group by p.analysisCategory
            """)
    List<CategoryCount> countByAnalysisCategory(@Param("userId") Long userId);
}
