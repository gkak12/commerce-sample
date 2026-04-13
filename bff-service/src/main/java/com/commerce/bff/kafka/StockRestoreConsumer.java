package com.commerce.bff.kafka;

import com.commerce.bff.stock.StockRedisService;
import com.commerce.common.event.StockRestoreEvent;
import com.commerce.common.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * stock.restore 토픽 소비
 * order-service가 주문 취소(Saga 보상) 시 발행 → Redis 재고 복원
 */
@Component
@RequiredArgsConstructor
public class StockRestoreConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockRestoreConsumer.class);

    private final StockRedisService stockRedisService;

    @KafkaListener(
            topics = KafkaTopic.STOCK_RESTORE,
            groupId = "bff-service-group"
    )
    public void consumeStockRestore(StockRestoreEvent event) {
        log.info("[Kafka][Saga] Stock restore event received. orderId={}, items={}",
                event.getOrderId(), event.getItems().size());

        event.getItems().forEach(item -> {
            stockRedisService.restoreStock(item.getProductId(), item.getQuantity());
            log.info("[Stock] Restored by Saga. orderId={}, productId={}, quantity={}",
                    event.getOrderId(), item.getProductId(), item.getQuantity());
        });
    }
}
