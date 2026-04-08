package com.commerce.point.service;

import com.commerce.common.event.PaymentCompletedEvent;

public interface PointService {
    void earnPoint(PaymentCompletedEvent event);
}
