package com.commerce.payment.controller;

import com.commerce.payment.dto.PaymentConfirmRequest;
import com.commerce.payment.dto.PaymentConfirmResponse;
import com.commerce.payment.dto.toss.TossWebhookRequest;
import com.commerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    /**
     * 클라이언트가 Toss 결제창에서 결제 완료 후 paymentKey, orderId, amount를 전달
     * Payment Service가 Toss 최종 승인 API 호출
     */
    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(@Valid @RequestBody PaymentConfirmRequest request) {
        PaymentConfirmResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Toss 결제 상태 변경 Webhook 수신
     * ngrok 등으로 외부 노출 후 Toss 개발자센터에 URL 등록 필요
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody TossWebhookRequest request) {
        log.info("[Webhook] Toss event received. eventType={}, orderId={}",
                request.getEventType(), request.getData().getOrderId());
        paymentService.handleWebhook(request);
        return ResponseEntity.ok().build();
    }
}
