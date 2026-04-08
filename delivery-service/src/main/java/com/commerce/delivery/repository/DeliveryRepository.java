package com.commerce.delivery.repository;

import com.commerce.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, String>, DeliveryRepositoryDsl {
}
