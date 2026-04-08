package com.commerce.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmResponse {

    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private String status;
    private String method;
    private String approvedAt;
}
