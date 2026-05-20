package com.commerce.catalog.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String optionName;          // 예: 색상, 사이즈

    @Column(nullable = false)
    private String optionValue;         // 예: 빨강, XL

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal additionalPrice; // 옵션 추가 금액

    @Column(nullable = false)
    private int stockQuantity;          // DB 기준 재고 (Redis와 주기적 동기화)

    public void updateStock(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}
