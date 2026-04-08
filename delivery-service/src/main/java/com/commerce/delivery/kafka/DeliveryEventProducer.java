package com.commerce.delivery.kafka;

import com.commerce.common.event.DeliveryStartedEvent;
import com.commerce.common.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryEventProducer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishDeliveryStarted(DeliveryStartedEvent event) {
        kafkaTemplate.send(KafkaTopic.DELIVERY_STARTED, event.getDeliveryId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[Kafka] Delivery started event published. deliveryId={}, offset={}",
                                event.getDeliveryId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("[Kafka] Failed to publish delivery started event. deliveryId={}",
                                event.getDeliveryId(), ex);
                    }
                });
    }
}
