package com.commerce.order.service.impl;

import com.commerce.common.event.OrderConfirmedEvent;
import com.commerce.common.event.OrderCreatedEvent;
import com.commerce.order.entity.Order;
import com.commerce.order.entity.OrderItem;
import com.commerce.order.entity.OrderStatus;
import com.commerce.order.kafka.OrderEventProducer;
import com.commerce.order.repository.OrderRepository;
import com.commerce.order.service.OrderService;
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
    private final OrderEventProducer orderEventProducer;

    @Override
    @Transactional
    public void processOrderCreated(OrderCreatedEvent event) {
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

        OrderConfirmedEvent confirmedEvent = OrderConfirmedEvent.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .totalAmount(event.getTotalAmount())
                .build();
        orderEventProducer.publishOrderConfirmed(confirmedEvent);
    }
}
