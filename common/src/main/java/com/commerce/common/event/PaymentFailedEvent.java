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
public class PaymentFailedEvent {

    private String orderId;
    private String userId;
    private String reason;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();
}
