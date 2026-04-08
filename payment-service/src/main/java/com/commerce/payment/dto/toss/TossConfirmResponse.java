package com.commerce.payment.dto.toss;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossConfirmResponse {

    private String paymentKey;
    private String orderId;
    private String orderName;
    /** DONE, CANCELED, PARTIAL_CANCELED, ABORTED, EXPIRED */
    private String status;
    /** 카드, 가상계좌, 간편결제, 휴대폰, 계좌이체, 문화상품권, ... */
    private String method;
    private BigDecimal totalAmount;
    /** ISO 8601 e.g. "2024-01-15T12:00:00+09:00" */
    private String approvedAt;
    private TossCardInfo card;
}
