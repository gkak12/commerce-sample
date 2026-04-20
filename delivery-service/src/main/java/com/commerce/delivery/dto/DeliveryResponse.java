package com.commerce.delivery.dto;

import java.time.LocalDateTime;

public record DeliveryResponse(
        String deliveryId,
        String orderId,
        String userId,
        String address,
        String status,
        LocalDateTime createdAt
) {}
