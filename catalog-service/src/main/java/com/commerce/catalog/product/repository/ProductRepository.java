package com.commerce.catalog.product.repository;

import com.commerce.catalog.product.entity.Product;
import com.commerce.catalog.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductId(String productId);
    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);
    Page<Product> findAllByCategoryIdAndStatus(Long categoryId, ProductStatus status, Pageable pageable);
    Page<Product> findAllBySellerIdAndStatus(String sellerId, ProductStatus status, Pageable pageable);
    Page<Product> findAllBySellerId(String sellerId, Pageable pageable);
}
