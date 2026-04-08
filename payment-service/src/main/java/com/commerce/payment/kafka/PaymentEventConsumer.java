package com.commerce.payment.kafka;

import com.commerce.common.event.OrderConfirmedEvent;
import com.commerce.common.kafka.KafkaTopic;
import com.commerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final PaymentService paymentService;

    @KafkaListener(
            topics = KafkaTopic.ORDER_CONFIRMED,
            groupId = "payment-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderConfirmed(OrderConfirmedEvent event) {
        log.info("[Kafka] Order confirmed event received. orderId={}", event.getOrderId());
        paymentService.processOrderConfirmed(event);
    }
}
