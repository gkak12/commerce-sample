package com.commerce.payment.dto.toss;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossWebhookData {

    private String paymentKey;
    private String orderId;
    /** DONE, CANCELED, PARTIAL_CANCELED, ABORTED, EXPIRED */
    private String status;
}
