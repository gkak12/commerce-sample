package com.commerce.order.kafka;

import com.commerce.common.event.OrderCreatedEvent;
import com.commerce.common.event.PaymentFailedEvent;
import com.commerce.common.kafka.KafkaTopic;
import com.commerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final OrderService orderService;

    @KafkaListener(
            topics = KafkaTopic.ORDER_CREATED,
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderCreated(OrderCreatedEvent event) {
        log.info("[Kafka] Order created event received. orderId={}", event.getOrderId());
        orderService.processOrderCreated(event);
    }

    /**
     * Saga 보상 트랜잭션: 결제 실패 시 주문 취소
     * payment-service가 payment.failed 발행 → 여기서 소비하여 주문을 CANCELLED로 변경
     */
    @KafkaListener(
            topics = KafkaTopic.PAYMENT_FAILED,
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentFailed(PaymentFailedEvent event) {
        log.warn("[Kafka][Saga] Payment failed. Cancelling order. orderId={}, reason={}",
                event.getOrderId(), event.getReason());
        orderService.cancelOrder(event.getOrderId());
    }
}
