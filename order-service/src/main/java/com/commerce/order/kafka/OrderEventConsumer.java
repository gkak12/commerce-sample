package com.commerce.order.kafka;

import com.commerce.common.event.OrderCancelRequestedEvent;
import com.commerce.common.event.OrderCreatedEvent;
import com.commerce.common.event.PaymentCompletedEvent;
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
     * 사용자 직접 취소 요청
     * bff-service가 order.cancel.requested 발행 → 취소 가능 상태 검증 후 cancelOrder()
     */
    @KafkaListener(
            topics = KafkaTopic.ORDER_CANCEL_REQUESTED,
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderCancelRequested(OrderCancelRequestedEvent event) {
        log.info("[Kafka] Order cancel requested. orderId={}, userId={}, reason={}",
                event.getOrderId(), event.getUserId(), event.getReason());
        orderService.cancelOrder(event.getOrderId(), event.getUserId(), event.getReason());
    }

    /**
     * 결제 완료 → 주문 상태 COMPLETED 변경 + order.completed 발행
     */
    @KafkaListener(
            topics = KafkaTopic.PAYMENT_COMPLETED,
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentCompleted(PaymentCompletedEvent event) {
        log.info("[Kafka] Payment completed. Completing order. orderId={}", event.getOrderId());
        orderService.completeOrder(event);
    }

    /**
     * Saga 보상 트랜잭션: 결제 실패 시 주문 취소
     * payment-service가 payment.failed 발행 → 주문 CANCELLED + order.cancelled 발행
     */
    @KafkaListener(
            topics = KafkaTopic.PAYMENT_FAILED,
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentFailed(PaymentFailedEvent event) {
        log.warn("[Kafka][Saga] Payment failed. Cancelling order. orderId={}, reason={}",
                event.getOrderId(), event.getReason());
        orderService.cancelOrder(event.getOrderId(), event.getReason());
    }
}
