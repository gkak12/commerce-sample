package com.commerce.point.service.impl;

import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.common.event.PointEarnedEvent;
import com.commerce.common.kafka.KafkaTopic;
import com.commerce.point.dto.PointBalanceResponse;
import com.commerce.point.entity.Point;
import com.commerce.point.entity.PointType;
import com.commerce.point.entity.PointWallet;
import com.commerce.point.outbox.OutboxEvent;
import com.commerce.point.outbox.OutboxEventRepository;
import com.commerce.point.repository.PointRepository;
import com.commerce.point.repository.PointWalletRepository;
import com.commerce.point.service.PointService;
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
public class PointServiceImpl implements PointService {

    private static final Logger log = LoggerFactory.getLogger(PointServiceImpl.class);

    private static final double POINT_RATE = 0.01; // 결제금액의 1% 적립

    private final PointRepository pointRepository;
    private final PointWalletRepository pointWalletRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    // ── 쿼리 ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PointBalanceResponse getPointBalance(String userId) {
        return pointWalletRepository.findById(userId)
                .map(PointBalanceResponse::from)
                .orElse(new PointBalanceResponse(userId, 0L));
    }

    // ── 커맨드 ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void earnPoint(PaymentCompletedEvent event) {
        // 멱등성 체크
        if (pointRepository.existsByOrderId(event.getOrderId())) {
            log.warn("[Point] Duplicate event ignored. orderId={}", event.getOrderId());
            return;
        }

        long earnedPoint = (long) (event.getAmount().doubleValue() * POINT_RATE);

        PointWallet wallet = pointWalletRepository.findById(event.getUserId())
                .orElseGet(() -> new PointWallet(event.getUserId()));
        wallet.earn(earnedPoint);
        pointWalletRepository.save(wallet);

        pointRepository.save(Point.builder()
                .userId(event.getUserId())
                .orderId(event.getOrderId())
                .earnedPoint(earnedPoint)
                .type(PointType.EARN)
                .build());

        log.info("[Point] Point earned. userId={}, earnedPoint={}, totalPoint={}",
                event.getUserId(), earnedPoint, wallet.getTotalPoint());

        outboxEventRepository.save(OutboxEvent.create(
                event.getOrderId(),
                KafkaTopic.POINT_EARNED,
                serialize(PointEarnedEvent.builder()
                        .pointId(UUID.randomUUID().toString())
                        .userId(event.getUserId())
                        .orderId(event.getOrderId())
                        .earnedPoint(earnedPoint)
                        .totalPoint(wallet.getTotalPoint())
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
