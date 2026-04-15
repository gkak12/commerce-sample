package com.commerce.bff.kafka;

import com.commerce.bff.stock.StockRedisService;
import com.commerce.common.event.StockRestoreEvent;
import com.commerce.common.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * stock.restore 토픽 소비
 * order-service가 주문 취소(Saga 보상) 시 발행 → Redis 재고 복원
 *
 * 멱등성 처리: Redis에 처리된 orderId를 키로 저장 (7일 TTL)
 * 동일 이벤트 재수신 시 중복 복원 방지
 */
@Component
@RequiredArgsConstructor
public class StockRestoreConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockRestoreConsumer.class);
    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:stock-restore:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final StockRedisService stockRedisService;
    private final RedisTemplate<String, String> redisTemplate;

    @KafkaListener(
            topics = KafkaTopic.STOCK_RESTORE,
            groupId = "bff-service-group"
    )
    public void consumeStockRestore(StockRestoreEvent event) {
        String idempotentKey = IDEMPOTENT_KEY_PREFIX + event.getOrderId();

        // 멱등성 체크: 이미 처리된 이벤트면 무시
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.warn("[Kafka][Idempotent] 중복 stock.restore 이벤트 무시. orderId={}", event.getOrderId());
            return;
        }

        log.info("[Kafka][Saga] Stock restore event received. orderId={}, items={}",
                event.getOrderId(), event.getItems().size());

        event.getItems().forEach(item -> {
            stockRedisService.restoreStock(item.getProductId(), item.getQuantity());
            log.info("[Stock] Restored by Saga. orderId={}, productId={}, quantity={}",
                    event.getOrderId(), item.getProductId(), item.getQuantity());
        });
    }
}
