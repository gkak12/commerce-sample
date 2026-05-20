package com.commerce.bff.kafka;

import com.commerce.bff.entity.Role;
import com.commerce.bff.entity.User;
import com.commerce.bff.repository.UserRepository;
import com.commerce.common.event.SellerApprovedEvent;
import com.commerce.common.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 판매자 승인 이벤트 컨슈머
 *
 * catalog-service가 판매자 승인 처리 후 발행
 * → bff-service users 테이블의 해당 유저 role을 USER → SELLER로 변경
 */
@Component
@RequiredArgsConstructor
public class SellerApprovedConsumer {

    private static final Logger log = LoggerFactory.getLogger(SellerApprovedConsumer.class);

    private final UserRepository userRepository;

    @KafkaListener(
            topics = KafkaTopic.SELLER_APPROVED,
            groupId = "bff-seller-role-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onSellerApproved(SellerApprovedEvent event) {
        log.info("[Seller] SellerApproved event received. userId={}, sellerId={}",
                event.getUserId(), event.getSellerId());

        User user = userRepository.findByUserId(event.getUserId())
                .orElseGet(() -> {
                    log.warn("[Seller] User not found for role update. userId={}", event.getUserId());
                    return null;
                });

        if (user == null) {
            return;
        }

        if (user.getRole() == Role.SELLER) {
            log.info("[Seller] User already has SELLER role — skipping. userId={}", event.getUserId());
            return;
        }

        user.updateRole(Role.SELLER);
        log.info("[Seller] User role updated to SELLER. userId={}, sellerId={}",
                event.getUserId(), event.getSellerId());
    }
}
