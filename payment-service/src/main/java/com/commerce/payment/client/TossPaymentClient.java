package com.commerce.payment.client;

import com.commerce.payment.config.TossPaymentProperties;
import com.commerce.payment.dto.toss.TossConfirmRequest;
import com.commerce.payment.dto.toss.TossConfirmResponse;
import com.commerce.payment.dto.toss.TossErrorResponse;
import com.commerce.payment.exception.TossPaymentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    private static final Logger log = LoggerFactory.getLogger(TossPaymentClient.class);

    private final WebClient tossWebClient;
    private final TossPaymentProperties properties;

    public TossConfirmResponse confirm(TossConfirmRequest request) {
        log.info("[Toss] Payment confirm request. orderId={}, amount={}", request.getOrderId(), request.getAmount());

        TossConfirmResponse response = tossWebClient.post()
                .uri(properties.getConfirmPath())
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse ->
                        clientResponse.bodyToMono(TossErrorResponse.class).map(error -> {
                            log.error("[Toss] Payment confirm failed. code={}, message={}", error.getCode(), error.getMessage());
                            return new TossPaymentException(error.getCode(), error.getMessage());
                        })
                )
                .bodyToMono(TossConfirmResponse.class)
                .block();

        if (response == null) {
            throw new TossPaymentException("EMPTY_RESPONSE", "토스 결제 확인 응답이 없습니다.");
        }
        return response;
    }
}
