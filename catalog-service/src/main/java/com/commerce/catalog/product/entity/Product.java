package com.commerce.catalog.product.entity;

import com.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productId;       // UUID (외부 노출용)

    @Column(nullable = false)
    private String sellerId;        // 판매자 UUID

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> options = new ArrayList<>();

    // ── 도메인 메서드 ────────────────────────────────────────────────────────

    public void update(String name, String description, BigDecimal basePrice, Long categoryId) {
        this.name        = name;
        this.description = description;
        this.basePrice   = basePrice;
        this.categoryId  = categoryId;
    }

    public void publish() {
        this.status = ProductStatus.ON_SALE;
    }

    public void suspend() {
        this.status = ProductStatus.SUSPENDED;
    }

    public void markSoldOut() {
        this.status = ProductStatus.SOLD_OUT;
    }

    public void markOnSale() {
        this.status = ProductStatus.ON_SALE;
    }
}
