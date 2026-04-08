package com.commerce.payment.dto.toss;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossCardInfo {

    private String issuerCode;
    private String acquirerCode;
    private String number;
    private Integer installmentPlanMonths;
    private String approveNo;
}
