package com.commerce.point.kafka;

import com.commerce.common.event.PointEarnedEvent;
import com.commerce.common.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PointEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPointEarned(PointEarnedEvent event) {
        kafkaTemplate.send(KafkaTopic.POINT_EARNED, event.getPointId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[Kafka] Point earned event published. pointId={}, offset={}",
                                event.getPointId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("[Kafka] Failed to publish point earned event. pointId={}", event.getPointId(), ex);
                    }
                });
    }
}
