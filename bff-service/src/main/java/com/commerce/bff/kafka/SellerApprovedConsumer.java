package com.commerce.bff.kafka;

import com.commerce.common.event.SellerApprovedEvent;
import com.commerce.common.kafka.KafkaTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 판매자 승인 이벤트 컨슈머
 *
 * catalog-service가 판매자 승인 처리 후 발행.
 * sellers 테이블이 분리되어 있으므로 bff-service에서 별도 role 업데이트 불필요.
 * 승인 후 알림 발송 등 부가 처리가 필요하면 이 곳에 추가.
 */
@Component
public class SellerApprovedConsumer {

    private static final Logger log = LoggerFactory.getLogger(SellerApprovedConsumer.class);

    @KafkaListener(
            topics = KafkaTopic.SELLER_APPROVED,
            groupId = "bff-seller-approved-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSellerApproved(SellerApprovedEvent event) {
        log.info("[Seller] Approved event received. sellerId={}", event.getSellerId());

        // ↓ 판매자 승인 알림 이메일/푸시 발송 위치
        // emailService.sendSellerApprovedMail(event.getEmail(), event.getBusinessName());
    }
}
