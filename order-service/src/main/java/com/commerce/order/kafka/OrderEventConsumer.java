package com.commerce.order.kafka;

import com.commerce.common.event.OrderCreatedEvent;
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
}
