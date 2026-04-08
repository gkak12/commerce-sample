package com.commerce.payment.entity;

import com.commerce.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment extends BaseEntity {

    @Id
    @Column(nullable = false, length = 36)
    private String paymentId;

    @Column(nullable = false, length = 36)
    private String orderId;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(length = 200)
    private String tossPaymentKey;

    @Column(length = 50)
    private String method;

    @Column(length = 50)
    private String approvedAt;

    protected Payment() {
    }

    public Payment(String paymentId, String orderId, String userId, BigDecimal amount, PaymentStatus status) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
    }

    public void complete(String tossPaymentKey, String method, String approvedAt) {
        this.status = PaymentStatus.COMPLETED;
        this.tossPaymentKey = tossPaymentKey;
        this.method = method;
        this.approvedAt = approvedAt;
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }
}
