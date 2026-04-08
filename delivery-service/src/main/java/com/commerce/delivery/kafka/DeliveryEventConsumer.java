package com.commerce.delivery.kafka;

import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.common.kafka.KafkaTopic;
import com.commerce.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventConsumer.class);

    private final DeliveryService deliveryService;

    @KafkaListener(
            topics = KafkaTopic.PAYMENT_COMPLETED,
            groupId = "delivery-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentCompleted(PaymentCompletedEvent event) {
        log.info("[Kafka] Payment completed event received. orderId={}", event.getOrderId());
        deliveryService.startDelivery(event);
    }
}
