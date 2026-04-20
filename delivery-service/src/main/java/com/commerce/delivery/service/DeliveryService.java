package com.commerce.delivery.service;

import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.delivery.dto.DeliveryResponse;

import java.util.Optional;

public interface DeliveryService {

    // ── 커맨드 ────────────────────────────────────────────────────────────────
    void startDelivery(PaymentCompletedEvent event);

    // ── 쿼리 ─────────────────────────────────────────────────────────────────
    Optional<DeliveryResponse> getDelivery(String orderId, String userId);
}
