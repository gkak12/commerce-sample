package com.commerce.delivery.repository;

import com.commerce.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, String>, DeliveryRepositoryDsl {

    boolean existsByOrderId(String orderId);

    Optional<Delivery> findByOrderId(String orderId);
}
