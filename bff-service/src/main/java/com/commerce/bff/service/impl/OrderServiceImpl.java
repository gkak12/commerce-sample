package com.commerce.bff.service.impl;

import com.commerce.bff.dto.OrderItemRequest;
import com.commerce.bff.dto.OrderRequest;
import com.commerce.bff.dto.OrderResponse;
import com.commerce.bff.kafka.OrderEventProducer;
import com.commerce.bff.service.OrderService;
import com.commerce.bff.stock.StockRedisService;
import com.commerce.bff.stock.StockRedisService.StockDecreaseItem;
import com.commerce.common.event.OrderCreatedEvent;
import com.commerce.common.event.OrderItem;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderEventProducer orderEventProducer;
    private final StockRedisService stockRedisService;

    @Override
    public OrderResponse placeOrder(OrderRequest request) {
        String orderId = UUID.randomUUID().toString();

        // 1. Redis 재고 선점 (원자적) — Kafka 발행 전에 먼저 처리
        List<StockDecreaseItem> stockItems = request.getItems().stream()
                .map(item -> new StockDecreaseItem(item.getProductId(), item.getQuantity()))
                .collect(Collectors.toList());

        boolean reserved = stockRedisService.decreaseStock(stockItems);
        if (!reserved) {
            log.warn("[Order] Stock reservation failed. orderId={}, userId={}", orderId, request.getUserId());
            return new OrderResponse(orderId, "재고가 부족합니다.");
        }

        log.info("[Order] Stock reserved. orderId={}, userId={}", orderId, request.getUserId());

        // 2. 재고 선점 성공 → Kafka 이벤트 발행
        try {
            List<OrderItem> items = request.getItems().stream()
                    .map(item -> new OrderItem(
                            item.getProductId(),
                            item.getProductName(),
                            item.getQuantity(),
                            item.getPrice()
                    ))
                    .collect(Collectors.toList());

            BigDecimal totalAmount = items.stream()
                    .reduce(BigDecimal.ZERO,
                            (acc, item) -> acc.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))),
                            BigDecimal::add);

            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(orderId)
                    .userId(request.getUserId())
                    .items(items)
                    .totalAmount(totalAmount)
                    .build();

            orderEventProducer.publishOrderCreated(event);

            return new OrderResponse(orderId, "주문이 접수되었습니다.");

        } catch (Exception e) {
            // Kafka 발행 실패 시 선점한 재고 즉시 롤백
            log.error("[Order] Kafka publish failed. Rolling back stock. orderId={}", orderId, e);
            request.getItems().forEach(item ->
                    stockRedisService.restoreStock(item.getProductId(), item.getQuantity())
            );
            throw new RuntimeException("주문 처리 중 오류가 발생했습니다.", e);
        }
    }
}
