package com.commerce.catalog.kafka;

import com.commerce.catalog.product.repository.ProductOptionRepository;
import com.commerce.common.event.OrderCancelledEvent;
import com.commerce.common.event.OrderConfirmedEvent;
import com.commerce.common.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 DB 동기화 컨슈머
 *
 * 실시간 재고: Redis (bff-service StockRedisService)
 * DB 기준값:  catalog-service product_options.stock_quantity
 *
 * 주문 확정 → DB 재고 차감
 * 주문 취소 → DB 재고 복구
 */
@Component
@RequiredArgsConstructor
public class StockEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockEventConsumer.class);

    private final ProductOptionRepository productOptionRepository;

    /**
     * 주문 확정 이벤트 소비 → DB 재고 차감
     * order-service가 결제 완료 후 발행
     */
    @KafkaListener(
            topics = KafkaTopic.ORDER_CONFIRMED,
            groupId = "catalog-stock-decrease-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("[Stock] Order confirmed. Decreasing DB stock. orderId={}", event.getOrderId());

        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("[Stock] No items in OrderConfirmedEvent. orderId={}", event.getOrderId());
            return;
        }

        event.getItems().forEach(item -> {
            int updated = productOptionRepository.decreaseStock(item.getProductId(), item.getQuantity());
            if (updated == 0) {
                log.warn("[Stock] Decrease skipped — stock insufficient or product not found. " +
                        "productId={}, quantity={}", item.getProductId(), item.getQuantity());
            } else {
                log.debug("[Stock] DB stock decreased. productId={}, quantity={}",
                        item.getProductId(), item.getQuantity());
            }
        });
    }

    /**
     * 주문 취소 이벤트 소비 → DB 재고 복구
     * order-service가 취소 처리 후 발행
     */
    @KafkaListener(
            topics = KafkaTopic.ORDER_CANCELLED,
            groupId = "catalog-stock-restore-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("[Stock] Order cancelled. Restoring DB stock. orderId={}", event.getOrderId());

        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("[Stock] No items in OrderCancelledEvent. orderId={}", event.getOrderId());
            return;
        }

        event.getItems().forEach(item -> {
            productOptionRepository.restoreStock(item.getProductId(), item.getQuantity());
            log.debug("[Stock] DB stock restored. productId={}, quantity={}",
                    item.getProductId(), item.getQuantity());
        });
    }
}
