package com.commerce.delivery.service.impl;

import com.commerce.common.event.DeliveryStartedEvent;
import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.delivery.entity.Delivery;
import com.commerce.delivery.entity.DeliveryStatus;
import com.commerce.delivery.kafka.DeliveryEventProducer;
import com.commerce.delivery.repository.DeliveryRepository;
import com.commerce.delivery.service.DeliveryService;
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
    private final DeliveryEventProducer deliveryEventProducer;

    @Transactional
    @Override
    public void startDelivery(PaymentCompletedEvent event) {
        String deliveryId = UUID.randomUUID().toString();

        // 실제 서비스에서는 주문 서비스에서 배송지 정보를 조회해야 함
        String address = "서울시 강남구 테헤란로 123";

        Delivery delivery = Delivery.builder()
                .deliveryId(deliveryId)
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .address(address)
                .status(DeliveryStatus.STARTED)
                .build();

        deliveryRepository.save(delivery);
        log.info("[Delivery] Delivery started. deliveryId={}, orderId={}", delivery.getDeliveryId(), delivery.getOrderId());

        DeliveryStartedEvent startedEvent = DeliveryStartedEvent.builder()
                .deliveryId(deliveryId)
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .address(address)
                .build();

        deliveryEventProducer.publishDeliveryStarted(startedEvent);
    }
}
