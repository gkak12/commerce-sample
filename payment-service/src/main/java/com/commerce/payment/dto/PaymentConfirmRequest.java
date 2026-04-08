package com.commerce.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmRequest {

    @NotBlank
    private String paymentKey;

    @NotBlank
    private String orderId;

    @Positive
    private BigDecimal amount;
}
