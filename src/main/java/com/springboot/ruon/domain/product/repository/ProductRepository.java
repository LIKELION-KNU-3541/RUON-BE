package com.springboot.ruon.domain.product.repository;

import com.springboot.ruon.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
