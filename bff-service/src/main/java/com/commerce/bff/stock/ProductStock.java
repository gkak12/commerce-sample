package com.commerce.bff.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_stock")
public class ProductStock {

    @Id
    @Column(name = "product_id")
    private String productId;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    protected ProductStock() {}

    public ProductStock(String productId, long quantity, LocalDateTime syncedAt) {
        this.productId = productId;
        this.quantity = quantity;
        this.syncedAt = syncedAt;
    }

    public String getProductId() { return productId; }
    public long getQuantity() { return quantity; }
    public LocalDateTime getSyncedAt() { return syncedAt; }

    public void update(long quantity, LocalDateTime syncedAt) {
        this.quantity = quantity;
        this.syncedAt = syncedAt;
    }
}
