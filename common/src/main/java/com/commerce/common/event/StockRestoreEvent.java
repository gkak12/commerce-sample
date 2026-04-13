package com.commerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 재고 복구 이벤트
 * order-service가 주문 취소(Saga 보상) 시 발행 → bff-service가 소비하여 Redis 재고 복원
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRestoreEvent {

    private String orderId;
    private String userId;
    private List<StockItem> items;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockItem {
        private String productId;
        private int quantity;
    }
}
