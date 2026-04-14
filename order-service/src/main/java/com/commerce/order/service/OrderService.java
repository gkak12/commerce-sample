package com.commerce.order.service;

import com.commerce.common.event.OrderCreatedEvent;
import com.commerce.common.event.PaymentCompletedEvent;

public interface OrderService {
    void processOrderCreated(OrderCreatedEvent event);
    void completeOrder(PaymentCompletedEvent event);
    void cancelOrder(String orderId, String reason);           // Saga 보상 (결제 실패)
    void cancelOrder(String orderId, String userId, String reason); // 사용자 직접 취소
}
