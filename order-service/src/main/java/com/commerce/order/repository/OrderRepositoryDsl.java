package com.commerce.order.repository;

import com.commerce.order.entity.Order;
import com.commerce.order.entity.OrderStatus;

import java.util.List;

public interface OrderRepositoryDsl {
    List<Order> findByUserIdAndStatus(String userId, OrderStatus status);
}
