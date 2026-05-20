package com.commerce.bff.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDto {
    private String orderId;
    private String status;
    private String totalAmount;
    private String createdAt;
}
