package com.commerce.bff.kafka;

import com.commerce.common.event.OrderCreatedEvent;
import com.commerce.common.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(KafkaTopic.ORDER_CREATED, event.getOrderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[Kafka] Order created event published. orderId={}, offset={}",
                                event.getOrderId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("[Kafka] Failed to publish order created event. orderId={}", event.getOrderId(), ex);
                    }
                });
    }
}
