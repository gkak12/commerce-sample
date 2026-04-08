package com.commerce.payment.dto.toss;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossWebhookRequest {

    /** PAYMENT_STATUS_CHANGED, DEPOSIT_CALLBACK 등 */
    private String eventType;
    private String createdAt;
    private TossWebhookData data;
}
