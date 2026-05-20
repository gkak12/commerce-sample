package com.commerce.catalog.kafka;

import com.commerce.common.event.SellerApprovedEvent;
import com.commerce.common.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SellerEventProducer {

    private static final Logger log = LoggerFactory.getLogger(SellerEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 판매자 승인 이벤트 발행
     * bff-service가 소비 → 해당 유저 role USER → SELLER 변경
     */
    public void publishSellerApproved(String userId, String sellerId) {
        SellerApprovedEvent event = SellerApprovedEvent.builder()
                .userId(userId)
                .sellerId(sellerId)
                .build();

        kafkaTemplate.send(KafkaTopic.SELLER_APPROVED, userId, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[Kafka] SellerApproved published. userId={}, sellerId={}", userId, sellerId);
                    } else {
                        log.error("[Kafka] SellerApproved publish failed. userId={}", userId, ex);
                    }
                });
    }
}
