package com.commerce.payment.kafka;

import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.common.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        kafkaTemplate.send(KafkaTopic.PAYMENT_COMPLETED, event.getPaymentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[Kafka] Payment completed event published. paymentId={}, offset={}",
                                event.getPaymentId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("[Kafka] Failed to publish payment completed event. paymentId={}",
                                event.getPaymentId(), ex);
                    }
                });
    }
}
