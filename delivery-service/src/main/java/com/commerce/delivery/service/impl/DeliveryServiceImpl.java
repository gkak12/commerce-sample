package com.commerce.delivery.service.impl;

import com.commerce.common.event.DeliveryStartedEvent;
import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.common.kafka.KafkaTopic;
import com.commerce.delivery.dto.DeliveryResponse;
import com.commerce.delivery.entity.Delivery;
import com.commerce.delivery.mapper.DeliveryMapper;
import com.commerce.delivery.outbox.OutboxEvent;
import com.commerce.delivery.outbox.OutboxEventRepository;
import com.commerce.delivery.repository.DeliveryRepository;
import com.commerce.delivery.service.DeliveryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryServiceImpl.class);

    private final DeliveryRepository deliveryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final DeliveryMapper deliveryMapper;

    // ── 쿼리 ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<DeliveryResponse> getDelivery(String orderId, String userId) {
        return deliveryRepository.findByOrderId(orderId)
                .filter(d -> d.getUserId().equals(userId))
                .map(deliveryMapper::toResponse);
    }

    // ── 커맨드 ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void startDelivery(PaymentCompletedEvent event) {
        // 멱등성 체크
        if (deliveryRepository.existsByOrderId(event.getOrderId())) {
            log.warn("[Delivery] Duplicate event ignored. orderId={}", event.getOrderId());
            return;
        }

        String deliveryId = UUID.randomUUID().toString();
        String address = "서울시 강남구 테헤란로 123"; // 실제는 주문 서비스에서 조회

        Delivery delivery = Delivery.builder()
                .deliveryId(deliveryId)
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .address(address)
                .build();   // status = STARTED 생성자에서 고정

        deliveryRepository.save(delivery);
        log.info("[Delivery] Delivery started. deliveryId={}, orderId={}", deliveryId, event.getOrderId());

        outboxEventRepository.save(OutboxEvent.create(
                event.getOrderId(),
                KafkaTopic.DELIVERY_STARTED,
                serialize(DeliveryStartedEvent.builder()
                        .deliveryId(deliveryId)
                        .orderId(event.getOrderId())
                        .userId(event.getUserId())
                        .address(address)
                        .build())));
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
