package com.commerce.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productId,
        String productName,
        int quantity,
        BigDecimal price
) {}
