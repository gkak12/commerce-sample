package com.commerce.order.service;

import com.commerce.common.event.OrderCreatedEvent;

public interface OrderService {
    void processOrderCreated(OrderCreatedEvent event);
}
