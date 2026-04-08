package com.commerce.delivery.service;

import com.commerce.common.event.PaymentCompletedEvent;

public interface DeliveryService {
    void startDelivery(PaymentCompletedEvent event);
}
