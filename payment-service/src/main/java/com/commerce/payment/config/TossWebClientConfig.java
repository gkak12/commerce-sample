package com.commerce.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

@Configuration
@EnableConfigurationProperties(TossPaymentProperties.class)
@RequiredArgsConstructor
public class TossWebClientConfig {

    private final TossPaymentProperties properties;

    @Bean
    public WebClient tossWebClient() {
        String encoded = Base64.getEncoder()
                .encodeToString((properties.getSecretKey() + ":").getBytes());

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Basic " + encoded)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
