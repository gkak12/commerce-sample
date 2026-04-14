package com.commerce.order.repository;

import com.commerce.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String>, OrderRepositoryDsl {
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
}
