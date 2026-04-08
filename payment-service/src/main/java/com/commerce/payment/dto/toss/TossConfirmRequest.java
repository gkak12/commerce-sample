package com.commerce.payment.dto.toss;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossConfirmRequest {

    private String paymentKey;
    private String orderId;
    private BigDecimal amount;
}
