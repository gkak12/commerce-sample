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
public class OrderCancelRequestedEvent {

    private String orderId;
    private String userId;   // 요청자 검증용 (본인 주문만 취소 가능)
    private String reason;

    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();
}
