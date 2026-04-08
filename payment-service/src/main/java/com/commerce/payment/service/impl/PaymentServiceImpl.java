package com.commerce.payment.service.impl;

import com.commerce.common.event.OrderConfirmedEvent;
import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.payment.client.TossPaymentClient;
import com.commerce.payment.dto.PaymentConfirmRequest;
import com.commerce.payment.dto.PaymentConfirmResponse;
import com.commerce.payment.dto.toss.TossConfirmRequest;
import com.commerce.payment.dto.toss.TossConfirmResponse;
import com.commerce.payment.dto.toss.TossWebhookRequest;
import com.commerce.payment.dto.toss.TossWebhookData;
import com.commerce.payment.entity.Payment;
import com.commerce.payment.entity.PaymentStatus;
import com.commerce.payment.exception.PaymentNotFoundException;
import com.commerce.payment.kafka.PaymentEventProducer;
import com.commerce.payment.repository.PaymentRepository;
import com.commerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final TossPaymentClient tossPaymentClient;

    /**
     * Kafka order.confirmed 이벤트 수신 → PENDING 상태로 결제 레코드 생성
     * 실제 결제 승인은 confirmPayment()에서 처리
     */
    @Transactional
    @Override
    public void processOrderConfirmed(OrderConfirmedEvent event) {
        String paymentId = UUID.randomUUID().toString();

        Payment payment = new Payment(
                paymentId,
                event.getOrderId(),
                event.getUserId(),
                event.getTotalAmount(),
                PaymentStatus.PENDING
        );

        paymentRepository.save(payment);
        log.info("[Payment] Pending payment created. paymentId={}, orderId={}", paymentId, event.getOrderId());
    }

    /**
     * 클라이언트가 Toss 결제창 완료 후 호출
     * 1. Toss 최종 승인 API 호출
     * 2. 결제 상태 COMPLETED 업데이트
     * 3. payment.completed Kafka 이벤트 발행
     */
    @Transactional
    @Override
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentNotFoundException(request.getOrderId()));

        TossConfirmRequest tossRequest = TossConfirmRequest.builder()
                .paymentKey(request.getPaymentKey())
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .build();

        TossConfirmResponse tossResponse = tossPaymentClient.confirm(tossRequest);
        log.info("[Toss] Payment confirmed. paymentKey={}, status={}", tossResponse.getPaymentKey(), tossResponse.getStatus());

        payment.complete(
                tossResponse.getPaymentKey(),
                tossResponse.getMethod(),
                tossResponse.getApprovedAt()
        );

        PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .build();
        paymentEventProducer.publishPaymentCompleted(completedEvent);

        return PaymentConfirmResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .method(payment.getMethod())
                .approvedAt(payment.getApprovedAt())
                .build();
    }

    /**
     * Toss Webhook 처리 (가상계좌 입금 완료 등 비동기 이벤트)
     */
    @Transactional
    @Override
    public void handleWebhook(TossWebhookRequest request) {
        TossWebhookData data = request.getData();
        Payment payment = paymentRepository.findByTossPaymentKey(data.getPaymentKey()).orElse(null);

        if (payment == null) {
            log.warn("[Webhook] Payment not found for paymentKey={}", data.getPaymentKey());
            return;
        }

        String status = data.getStatus();
        if ("DONE".equals(status)) {
            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                payment.complete(data.getPaymentKey(), null, null);
                PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                        .paymentId(payment.getPaymentId())
                        .orderId(payment.getOrderId())
                        .userId(payment.getUserId())
                        .amount(payment.getAmount())
                        .build();
                paymentEventProducer.publishPaymentCompleted(completedEvent);
                log.info("[Webhook] Payment completed via webhook. orderId={}", payment.getOrderId());
            }
        } else if ("ABORTED".equals(status) || "EXPIRED".equals(status)) {
            payment.fail();
            log.warn("[Webhook] Payment failed. status={}, orderId={}", data.getStatus(), payment.getOrderId());
        } else {
            log.info("[Webhook] Unhandled status={}. orderId={}", data.getStatus(), payment.getOrderId());
        }
    }
}
