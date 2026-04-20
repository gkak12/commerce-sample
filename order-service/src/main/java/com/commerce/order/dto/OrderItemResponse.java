package com.commerce.order.dto;

import com.commerce.order.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productId,
        String productName,
        int quantity,
        BigDecimal price
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice()
        );
    }
}
