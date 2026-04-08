package com.commerce.bff.service.impl;

import com.commerce.bff.dto.OrderItemRequest;
import com.commerce.bff.dto.OrderRequest;
import com.commerce.bff.dto.OrderResponse;
import com.commerce.bff.kafka.OrderEventProducer;
import com.commerce.bff.service.OrderService;
import com.commerce.common.event.OrderCreatedEvent;
import com.commerce.common.event.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderEventProducer orderEventProducer;

    @Override
    public OrderResponse placeOrder(OrderRequest request) {
        String orderId = UUID.randomUUID().toString();

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
    }
}
