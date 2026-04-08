package com.commerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStartedEvent {

    private String deliveryId;
    private String orderId;
    private String userId;
    private String address;

    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();
}
