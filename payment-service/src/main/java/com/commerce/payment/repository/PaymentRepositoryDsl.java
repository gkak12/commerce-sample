package com.commerce.payment.repository;

import com.commerce.payment.entity.Payment;

import java.util.List;

public interface PaymentRepositoryDsl {
    List<Payment> findByUserId(String userId);
}
