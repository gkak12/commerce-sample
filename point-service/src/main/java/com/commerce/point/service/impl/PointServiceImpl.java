package com.commerce.point.service.impl;

import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.common.event.PointEarnedEvent;
import com.commerce.point.entity.Point;
import com.commerce.point.entity.PointType;
import com.commerce.point.entity.PointWallet;
import com.commerce.point.kafka.PointEventProducer;
import com.commerce.point.repository.PointRepository;
import com.commerce.point.repository.PointWalletRepository;
import com.commerce.point.service.PointService;
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

    // 결제금액의 1% 포인트 적립
    private static final double POINT_RATE = 0.01;

    private final PointRepository pointRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointEventProducer pointEventProducer;

    @Override
    @Transactional
    public void earnPoint(PaymentCompletedEvent event) {
        long earnedPoint = (long) (event.getAmount().doubleValue() * POINT_RATE);

        PointWallet wallet = pointWalletRepository.findById(event.getUserId())
                .orElseGet(() -> new PointWallet(event.getUserId(), 0L));

        wallet.earn(earnedPoint);
        pointWalletRepository.save(wallet);

        Point point = Point.builder()
                .userId(event.getUserId())
                .orderId(event.getOrderId())
                .earnedPoint(earnedPoint)
                .type(PointType.EARN)
                .build();
        pointRepository.save(point);

        log.info("[Point] Point earned. userId={}, earnedPoint={}, totalPoint={}",
                event.getUserId(), earnedPoint, wallet.getTotalPoint());

        String pointId = UUID.randomUUID().toString();
        PointEarnedEvent earnedEvent = PointEarnedEvent.builder()
                .pointId(pointId)
                .userId(event.getUserId())
                .orderId(event.getOrderId())
                .earnedPoint(earnedPoint)
                .totalPoint(wallet.getTotalPoint())
                .build();
        pointEventProducer.publishPointEarned(earnedEvent);
    }
}
