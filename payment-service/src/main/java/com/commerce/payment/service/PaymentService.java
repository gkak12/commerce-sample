package com.commerce.payment.service;

import com.commerce.common.event.OrderConfirmedEvent;
import com.commerce.payment.dto.PaymentConfirmRequest;
import com.commerce.payment.dto.PaymentConfirmResponse;
import com.commerce.payment.dto.toss.TossWebhookRequest;

public interface PaymentService {
    void processOrderConfirmed(OrderConfirmedEvent event);
    PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request);
    void handleWebhook(TossWebhookRequest request);
}
