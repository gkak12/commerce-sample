package com.commerce.payment.repository;

import com.commerce.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String>, PaymentRepositoryDsl {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByTossPaymentKey(String tossPaymentKey);
}
