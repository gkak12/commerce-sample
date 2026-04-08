package com.commerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEvent {

    private String orderId;
    private String userId;
    private BigDecimal totalAmount;

    @Builder.Default
    private LocalDateTime confirmedAt = LocalDateTime.now();
}
