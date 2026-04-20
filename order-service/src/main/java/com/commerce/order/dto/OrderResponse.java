package com.commerce.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        String orderId,
        String userId,
        BigDecimal totalAmount,
        String status,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {}
