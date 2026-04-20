package com.commerce.order.service;

import com.commerce.common.event.OrderCreatedEvent;
import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.order.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderService {

    // ── 커맨드 ────────────────────────────────────────────────────────────────
    void processOrderCreated(OrderCreatedEvent event);
    void completeOrder(PaymentCompletedEvent event);
    void cancelOrder(String orderId, String reason);
    void cancelOrder(String orderId, String userId, String reason);

    // ── 쿼리 ─────────────────────────────────────────────────────────────────
    Optional<OrderResponse> getOrder(String orderId, String userId);
    Page<OrderResponse> getOrderList(String userId, Pageable pageable);
}
