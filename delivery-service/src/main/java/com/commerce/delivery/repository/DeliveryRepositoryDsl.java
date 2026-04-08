package com.commerce.delivery.repository;

import com.commerce.delivery.entity.Delivery;

import java.util.List;

public interface DeliveryRepositoryDsl {
    List<Delivery> findByUserId(String userId);
}
