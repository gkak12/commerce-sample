package com.commerce.catalog.product.entity;

public enum ProductStatus {
    DRAFT,      // 등록 임시저장
    ON_SALE,    // 판매 중
    SOLD_OUT,   // 품절
    SUSPENDED   // 판매 중지 (물리 삭제 대신 사용)
}
