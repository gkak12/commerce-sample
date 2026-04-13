package com.commerce.payment.service.impl;

import com.commerce.common.event.OrderConfirmedEvent;
import com.commerce.common.event.PaymentCompletedEvent;
import com.commerce.common.event.PaymentFailedEvent;
import com.commerce.common.kafka.KafkaTopic;
import com.commerce.payment.client.TossPaymentClient;
import com.commerce.payment.dto.PaymentConfirmRequest;
import com.commerce.payment.dto.PaymentConfirmResponse;
import com.commerce.payment.dto.toss.TossConfirmRequest;
import com.commerce.payment.dto.toss.TossConfirmResponse;
import com.commerce.payment.dto.toss.TossWebhookData;
import com.commerce.payment.dto.toss.TossWebhookRequest;
import com.commerce.payment.entity.Payment;
import com.commerce.payment.entity.PaymentStatus;
import com.commerce.payment.exception.PaymentNotFoundException;
import com.commerce.payment.outbox.OutboxEvent;
import com.commerce.payment.outbox.OutboxEventRepository;
import com.commerce.payment.repository.PaymentRepository;
import com.commerce.payment.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final OutboxEventRepository outboxEventRepository;
    private final TossPaymentClient tossPaymentClient;
    private final ObjectMapper objectMapper;

    /**
     * Kafka order.confirmed 이벤트 수신 → PENDING 결제 레코드 생성
     */
    @Transactional
    @Override
    public void processOrderConfirmed(OrderConfirmedEvent event) {
        // 멱등성 체크
        if (paymentRepository.findByOrderId(event.getOrderId()).isPresent()) {
            log.warn("[Payment] Duplicate event ignored. orderId={}", event.getOrderId());
            return;
        }

        String paymentId = UUID.randomUUID().toString();
        paymentRepository.save(new Payment(
                paymentId,
                event.getOrderId(),
                event.getUserId(),
                event.getTotalAmount(),
                PaymentStatus.PENDING
        ));
        log.info("[Payment] Pending payment created. paymentId={}, orderId={}", paymentId, event.getOrderId());
    }

    /**
     * 클라이언트가 Toss 결제창 완료 후 호출
     * Saga 패턴: 실패 시 payment.failed 이벤트 발행 → order-service 보상 트랜잭션
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

        try {
            TossConfirmResponse tossResponse = tossPaymentClient.confirm(tossRequest);
            log.info("[Toss] Payment confirmed. paymentKey={}, status={}", tossResponse.getPaymentKey(), tossResponse.getStatus());

            payment.complete(tossResponse.getPaymentKey(), tossResponse.getMethod(), tossResponse.getApprovedAt());

            // Outbox 패턴: payment.completed 이벤트 적재
            PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                    .paymentId(payment.getPaymentId())
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .build();
            outboxEventRepository.save(OutboxEvent.create(
                    payment.getOrderId(), KafkaTopic.PAYMENT_COMPLETED, serialize(completedEvent)));

            return PaymentConfirmResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .orderId(payment.getOrderId())
                    .amount(payment.getAmount())
                    .status(payment.getStatus().name())
                    .method(payment.getMethod())
                    .approvedAt(payment.getApprovedAt())
                    .build();

        } catch (Exception e) {
            // Saga 보상 트랜잭션: 결제 실패 → payment.failed 이벤트로 주문 취소 유도
            log.error("[Payment] Toss payment failed. orderId={}", payment.getOrderId(), e);
            payment.fail();

            PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .reason(e.getMessage())
                    .build();
            outboxEventRepository.save(OutboxEvent.create(
                    payment.getOrderId(), KafkaTopic.PAYMENT_FAILED, serialize(failedEvent)));

            throw new RuntimeException("Payment failed: " + e.getMessage(), e);
        }
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
                outboxEventRepository.save(OutboxEvent.create(
                        payment.getOrderId(), KafkaTopic.PAYMENT_COMPLETED, serialize(completedEvent)));
                log.info("[Webhook] Payment completed via webhook. orderId={}", payment.getOrderId());
            }
        } else if ("ABORTED".equals(status) || "EXPIRED".equals(status)) {
            payment.fail();

            // Saga 보상 트랜잭션: 웹훅으로 실패 통보 시에도 주문 취소 유도
            PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .reason("Payment " + status + " via webhook")
                    .build();
            outboxEventRepository.save(OutboxEvent.create(
                    payment.getOrderId(), KafkaTopic.PAYMENT_FAILED, serialize(failedEvent)));
            log.warn("[Webhook] Payment failed. status={}, orderId={}", data.getStatus(), payment.getOrderId());
        } else {
            log.info("[Webhook] Unhandled status={}. orderId={}", data.getStatus(), payment.getOrderId());
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
