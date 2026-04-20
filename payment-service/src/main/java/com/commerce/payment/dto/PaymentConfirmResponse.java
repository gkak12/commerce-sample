package com.commerce.payment.dto;

import java.math.BigDecimal;

public record PaymentConfirmResponse(
        String paymentId,
        String orderId,
        BigDecimal amount,
        String status,
        String method,
        String approvedAt
) {}
