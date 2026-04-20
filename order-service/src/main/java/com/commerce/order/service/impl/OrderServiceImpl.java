package com.commerce.order.service.impl;

import com.commerce.common.event.OrderCancelledEvent;
import com.commerce.common.event.OrderCompletedEvent;
import com.commerce.common.event.OrderConfirmedEvent;
import com.commerce.common.event.OrderCreatedEvent;
import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.common.event.StockRestoreEvent;
import com.commerce.common.kafka.KafkaTopic;
import com.commerce.order.entity.Order;
import com.commerce.order.entity.OrderStatus;
import com.commerce.order.outbox.OutboxEvent;
import com.commerce.order.outbox.OutboxEventRepository;
import com.commerce.order.repository.OrderRepository;
import com.commerce.order.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void processOrderCreated(OrderCreatedEvent event) {
        // 멱등성 체크
        if (orderRepository.existsById(event.getOrderId())) {
            log.warn("[Order] Duplicate event ignored. orderId={}", event.getOrderId());
            return;
        }

        Order order = Order.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .totalAmount(event.getTotalAmount())
                .status(OrderStatus.CONFIRMED)
                .build();

        // Order가 직접 아이템 생성 → 양방향 관계 Order 내부에서 처리
        event.getItems().forEach(item ->
                order.addItem(item.getProductId(), item.getProductName(),
                              item.getQuantity(), item.getPrice()));

        orderRepository.save(order);
        log.info("[Order] Order saved. orderId={}, userId={}, itemCount={}",
                order.getOrderId(), order.getUserId(), order.getOrderItems().size());

        // Outbox: order.confirmed 발행
        OrderConfirmedEvent confirmedEvent = OrderConfirmedEvent.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .totalAmount(event.getTotalAmount())
                .build();

        outboxEventRepository.save(OutboxEvent.create(
                event.getOrderId(),
                KafkaTopic.ORDER_CONFIRMED,
                serialize(confirmedEvent)
        ));
    }

    @Override
    @Transactional
    public void completeOrder(PaymentCompletedEvent event) {
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.COMPLETED) {
                log.warn("[Order] Already completed. orderId={}", event.getOrderId());
                return;
            }

            order.complete();
            log.info("[Order] Order completed. orderId={}, userId={}", order.getOrderId(), order.getUserId());

            // order.completed 발행 → 알림 서비스 구독
            OrderCompletedEvent completedEvent = OrderCompletedEvent.builder()
                    .orderId(order.getOrderId())
                    .userId(order.getUserId())
                    .totalAmount(order.getTotalAmount())
                    .build();

            outboxEventRepository.save(OutboxEvent.create(
                    order.getOrderId(),
                    KafkaTopic.ORDER_COMPLETED,
                    serialize(completedEvent)
            ));
        });
    }

    /** 사용자 직접 취소: userId 본인 검증 + 취소 가능 상태 검증 */
    @Override
    @Transactional
    public void cancelOrder(String orderId, String userId, String reason) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (!order.getUserId().equals(userId)) {
                log.warn("[Order] Unauthorized cancel attempt. orderId={}, requestUserId={}", orderId, userId);
                return;
            }
            if (order.getStatus() == OrderStatus.DELIVERING
                    || order.getStatus() == OrderStatus.DELIVERED
                    || order.getStatus() == OrderStatus.COMPLETED
                    || order.getStatus() == OrderStatus.CANCELLED) {
                log.warn("[Order] Cannot cancel. orderId={}, status={}", orderId, order.getStatus());
                return;
            }
            cancelOrderInternal(order, reason);
        });
    }

    /** Saga 보상 취소: 결제 실패로 인한 시스템 취소 */
    @Override
    @Transactional
    public void cancelOrder(String orderId, String reason) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                log.warn("[Order] Already cancelled. orderId={}", orderId);
                return;
            }
            cancelOrderInternal(order, reason);
        });
    }

    // ── private ──────────────────────────────────────────────────────────────

    private void cancelOrderInternal(Order order, String reason) {
        order.cancel();
        log.info("[Order] Order cancelled. orderId={}, reason={}", order.getOrderId(), reason);

        List<StockRestoreEvent.StockItem> stockItems = order.getOrderItems().stream()
                .map(item -> StockRestoreEvent.StockItem.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        // stock.restore 발행 → bff-service Redis 재고 복구
        outboxEventRepository.save(OutboxEvent.create(
                order.getOrderId(),
                KafkaTopic.STOCK_RESTORE,
                serialize(StockRestoreEvent.builder()
                        .orderId(order.getOrderId())
                        .userId(order.getUserId())
                        .items(stockItems)
                        .build())
        ));

        // order.cancelled 발행 → 취소 이메일
        outboxEventRepository.save(OutboxEvent.create(
                order.getOrderId(),
                KafkaTopic.ORDER_CANCELLED,
                serialize(OrderCancelledEvent.builder()
                        .orderId(order.getOrderId())
                        .userId(order.getUserId())
                        .reason(reason)
                        .build())
        ));

        log.info("[Order] Stock restore + cancelled events queued. orderId={}", order.getOrderId());
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
