package com.commerce.bff.mail;

import com.commerce.bff.repository.UserRepository;
import com.commerce.common.event.OrderCancelledEvent;
import com.commerce.common.event.OrderCompletedEvent;
import com.commerce.common.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 주문 완료/취소 이메일 알림 Consumer
 *
 * - order.completed → 주문 완료 메일 (order-service가 payment.completed 수신 후 발행)
 * - order.cancelled → 주문 취소 메일 (order-service가 payment.failed 수신 후 발행)
 *
 * payment 도메인 이벤트가 아닌 order 도메인 이벤트를 구독 → 도메인 경계 준수
 * order-service에서 상태 변경이 완료된 후 발행되므로 이메일 발송 시점 보장
 */
@Component
@RequiredArgsConstructor
public class OrderNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationConsumer.class);

    private final UserRepository userRepository;
    private final EmailService emailService;

    @KafkaListener(
            topics = KafkaTopic.ORDER_COMPLETED,
            groupId = "bff-notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCompleted(OrderCompletedEvent event) {
        log.info("[Notification] 주문 완료 이벤트 수신. orderId={}, userId={}", event.getOrderId(), event.getUserId());

        userRepository.findByUserId(event.getUserId())
                .ifPresentOrElse(
                        user -> emailService.sendOrderCompletedMail(
                                user.getEmail(),
                                event.getOrderId(),
                                event.getTotalAmount(),
                                event.getCompletedAt()
                        ),
                        () -> log.warn("[Notification] 유저를 찾을 수 없음. userId={}", event.getUserId())
                );
    }

    @KafkaListener(
            topics = KafkaTopic.ORDER_CANCELLED,
            groupId = "bff-notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("[Notification] 주문 취소 이벤트 수신. orderId={}, userId={}", event.getOrderId(), event.getUserId());

        userRepository.findByUserId(event.getUserId())
                .ifPresentOrElse(
                        user -> emailService.sendOrderCancelledMail(
                                user.getEmail(),
                                event.getOrderId(),
                                event.getReason()
                        ),
                        () -> log.warn("[Notification] 유저를 찾을 수 없음. userId={}", event.getUserId())
                );
    }
}
