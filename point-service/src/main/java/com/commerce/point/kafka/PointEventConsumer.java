package com.commerce.point.kafka;

import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.common.kafka.KafkaTopic;
import com.commerce.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PointEventConsumer.class);

    private final PointService pointService;

    @KafkaListener(
            topics = KafkaTopic.PAYMENT_COMPLETED,
            groupId = "point-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentCompleted(PaymentCompletedEvent event) {
        log.info("[Kafka] Payment completed event received. orderId={}", event.getOrderId());
        pointService.earnPoint(event);
    }
}
