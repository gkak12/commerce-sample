package com.commerce.delivery.service.impl;

import com.commerce.common.event.DeliveryStartedEvent;
import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.common.kafka.KafkaTopic;
import com.commerce.delivery.entity.Delivery;
import com.commerce.delivery.entity.DeliveryStatus;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryServiceImpl.class);

    private final DeliveryRepository deliveryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
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
                .status(DeliveryStatus.STARTED)
                .build();

        deliveryRepository.save(delivery);
        log.info("[Delivery] Delivery started. deliveryId={}, orderId={}", deliveryId, event.getOrderId());

        // Outbox 패턴: delivery.started 이벤트 적재
        DeliveryStartedEvent startedEvent = DeliveryStartedEvent.builder()
                .deliveryId(deliveryId)
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .address(address)
                .build();

        outboxEventRepository.save(OutboxEvent.create(
                event.getOrderId(), KafkaTopic.DELIVERY_STARTED, serialize(startedEvent)));
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
