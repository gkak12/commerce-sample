package com.commerce.payment.dto.toss;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossErrorResponse {

    private String code;
    private String message;
}
