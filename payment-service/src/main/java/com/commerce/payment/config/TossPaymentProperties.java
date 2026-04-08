package com.commerce.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.payment")
@Getter
@Setter
public class TossPaymentProperties {

    private String secretKey;
    private String baseUrl = "https://api.tosspayments.com";
    private String confirmPath = "/v1/payments/confirm";
}
