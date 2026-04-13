package com.commerce.order.service.impl;

import com.commerce.common.event.OrderConfirmedEvent;
import com.commerce.common.event.OrderCreatedEvent;
import com.commerce.common.event.StockRestoreEvent;
import com.commerce.common.kafka.KafkaTopic;
import com.commerce.order.entity.Order;
import com.commerce.order.entity.OrderItem;
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

        List<OrderItem> orderItems = event.getItems().stream()
                .map(item -> OrderItem.builder()
                        .order(order)
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        orderRepository.save(order);
        log.info("[Order] Order saved. orderId={}, userId={}", order.getOrderId(), order.getUserId());

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
    public void cancelOrder(String orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                log.warn("[Order] Already cancelled. orderId={}", orderId);
                return;
            }

            order.setStatus(OrderStatus.CANCELLED);
            log.info("[Order] Order cancelled by Saga compensation. orderId={}", orderId);

            // Saga 보상: stock.restore 발행 → bff-service Redis 재고 복구
            List<StockRestoreEvent.StockItem> stockItems = order.getOrderItems().stream()
                    .map(item -> StockRestoreEvent.StockItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());

            StockRestoreEvent restoreEvent = StockRestoreEvent.builder()
                    .orderId(orderId)
                    .userId(order.getUserId())
                    .items(stockItems)
                    .build();

            outboxEventRepository.save(OutboxEvent.create(
                    orderId,
                    KafkaTopic.STOCK_RESTORE,
                    serialize(restoreEvent)
            ));

            log.info("[Order][Saga] Stock restore event queued. orderId={}, items={}",
                    orderId, stockItems.size());
        });
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
