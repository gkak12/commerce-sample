package com.commerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    private String orderId;
    private String userId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
