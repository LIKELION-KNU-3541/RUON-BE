package com.springboot.ruon.domain.product.repository;

import com.springboot.ruon.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 루틴 생성 시 유저가 화장대에 등록해둔 제품 목록을 읽어오기 위해 사용
    List<Product> findByUserId(Long userId);
}
