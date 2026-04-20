package com.commerce.bff.mail;

import com.commerce.bff.cache.MyPageCacheService;
import com.commerce.bff.repository.UserRepository;
import com.commerce.common.event.OrderCancelledEvent;
import com.commerce.common.event.OrderCompletedEvent;
import com.commerce.common.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 주문 완료/취소 이메일 알림 Consumer
 *
 * - order.completed → 주문 완료 메일 + 캐시 evict
 * - order.cancelled → 주문 취소 메일 + 캐시 evict
 *
 * 캐시 무효화:
 *   주문 상태 변경 이벤트 수신 시 해당 사용자의 주문 목록/상세 캐시를 즉시 삭제
 *   → 다음 조회 시 최신 상태를 gRPC로 가져와 재캐싱
 *
 * 멱등성 처리: Redis에 처리된 orderId를 키로 저장 (7일 TTL) → 중복 메일 방지
 */
@Component
@RequiredArgsConstructor
public class OrderNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationConsumer.class);
    private static final String COMPLETED_KEY_PREFIX = "idempotent:order-completed:";
    private static final String CANCELLED_KEY_PREFIX  = "idempotent:order-cancelled:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;
    private final MyPageCacheService cacheService;

    @KafkaListener(
            topics = KafkaTopic.ORDER_COMPLETED,
            groupId = "bff-notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCompleted(OrderCompletedEvent event) {
        String idempotentKey = COMPLETED_KEY_PREFIX + event.getOrderId();

        // 멱등성 체크: 이미 처리된 이벤트면 무시 (중복 메일 방지)
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.warn("[Notification][Idempotent] 중복 order.completed 이벤트 무시. orderId={}", event.getOrderId());
            return;
        }

        log.info("[Notification] 주문 완료 이벤트 수신. orderId={}, userId={}", event.getOrderId(), event.getUserId());

        // 캐시 무효화: 주문 상태가 COMPLETED로 변경됐으므로 이전 캐시 삭제
        cacheService.evictOrderCaches(event.getUserId(), event.getOrderId());

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
        String idempotentKey = CANCELLED_KEY_PREFIX + event.getOrderId();

        // 멱등성 체크: 이미 처리된 이벤트면 무시 (중복 메일 방지)
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.warn("[Notification][Idempotent] 중복 order.cancelled 이벤트 무시. orderId={}", event.getOrderId());
            return;
        }

        log.info("[Notification] 주문 취소 이벤트 수신. orderId={}, userId={}", event.getOrderId(), event.getUserId());

        // 캐시 무효화: 주문 상태가 CANCELLED로 변경됐으므로 이전 캐시 삭제
        cacheService.evictOrderCaches(event.getUserId(), event.getOrderId());

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
