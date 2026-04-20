package com.commerce.delivery.entity;

import com.commerce.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "deliveries")
@Getter
@NoArgsConstructor
public class Delivery extends BaseEntity {

    @Id
    @Column(nullable = false, length = 36)
    private String deliveryId;

    @Column(nullable = false, length = 36)
    private String orderId;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @Builder
    public Delivery(String deliveryId, String orderId, String userId, String address) {
        this.deliveryId = deliveryId;
        this.orderId = orderId;
        this.userId = userId;
        this.address = address;
        this.status = DeliveryStatus.STARTED;   // 초기 상태 고정
    }

    // ── 상태 변경 비즈니스 메서드 ─────────────────────────────────────────────

    public void startTransit() {
        this.status = DeliveryStatus.IN_TRANSIT;
    }

    public void complete() {
        this.status = DeliveryStatus.DELIVERED;
    }

    public void fail() {
        this.status = DeliveryStatus.FAILED;
    }
}
