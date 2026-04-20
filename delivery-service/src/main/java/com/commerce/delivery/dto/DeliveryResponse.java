package com.commerce.delivery.dto;

import com.commerce.delivery.entity.Delivery;

import java.time.LocalDateTime;

public record DeliveryResponse(
        String deliveryId,
        String orderId,
        String userId,
        String address,
        String status,
        LocalDateTime createdAt
) {
    public static DeliveryResponse from(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getDeliveryId(),
                delivery.getOrderId(),
                delivery.getUserId(),
                delivery.getAddress(),
                delivery.getStatus().name(),
                delivery.getCreatedAt()
        );
    }
}
