package com.commerce.catalog.product.service;

import com.commerce.catalog.product.dto.ProductCreateRequest;
import com.commerce.catalog.product.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    /** 상품 등록 (APPROVED 판매자만 가능) */
    ProductResponse create(String sellerId, ProductCreateRequest request);

    /** 상품 수정 (본인 상품만) */
    ProductResponse update(String sellerId, String productId, ProductCreateRequest request);

    /** 상품 삭제 — SUSPENDED 처리 (물리 삭제 금지) */
    void delete(String sellerId, String productId);

    /** 내 상품 목록 */
    Page<ProductResponse> getMyProducts(String sellerId, Pageable pageable);

    /** 상품 상세 조회 (공개) */
    ProductResponse getProduct(String productId);

    /** 상품 목록 조회 (공개, 카테고리 필터) */
    Page<ProductResponse> getProducts(Long categoryId, Pageable pageable);

    /** 상품 강제 중지 (관리자) */
    void suspendByAdmin(String productId);
}
