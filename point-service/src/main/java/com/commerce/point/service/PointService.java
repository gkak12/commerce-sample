package com.commerce.point.service;

import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.point.dto.PointBalanceResponse;

public interface PointService {

    // ── 커맨드 ────────────────────────────────────────────────────────────────
    void earnPoint(PaymentCompletedEvent event);

    // ── 쿼리 ─────────────────────────────────────────────────────────────────
    PointBalanceResponse getPointBalance(String userId);
}
