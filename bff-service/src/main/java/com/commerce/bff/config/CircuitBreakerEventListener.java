package com.commerce.bff.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 서킷 브레이커 상태 변경 이벤트 리스너
 *
 * 상태 전이 시 로그 출력:
 *   CLOSED  → OPEN       : 장애 감지 (알림 필요)
 *   OPEN    → HALF_OPEN  : 복구 시도 시작
 *   HALF_OPEN → CLOSED   : 정상 복구 완료
 *   HALF_OPEN → OPEN     : 복구 실패, 재차단
 */
@Component
public class CircuitBreakerEventListener {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerEventListener.class);

    private static final List<String> CIRCUIT_BREAKER_NAMES =
            List.of("order-service", "point-service", "delivery-service", "redis");

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerEventListener(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * 애플리케이션 시작 후 각 서킷 브레이커에 이벤트 리스너 등록
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerEventListeners() {
        CIRCUIT_BREAKER_NAMES.forEach(name -> {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
            registerStateTransitionListener(cb);
            registerFailureRateListener(cb);
        });
        log.info("[CircuitBreaker] 이벤트 리스너 등록 완료: {}", CIRCUIT_BREAKER_NAMES);
    }

    // ── 상태 전이 이벤트 ──────────────────────────────────────────────────────

    private void registerStateTransitionListener(CircuitBreaker cb) {
        cb.getEventPublisher().onStateTransition(event -> {
            CircuitBreaker.StateTransition transition = event.getStateTransition();
            String name = event.getCircuitBreakerName();

            switch (transition.getToState()) {
                case OPEN -> log.error(
                        "[CircuitBreaker] ⚡ 서킷 OPEN: {} ({} → {}). " +
                        "해당 서비스 장애 감지. fallback 응답 반환 중.",
                        name,
                        transition.getFromState(),
                        transition.getToState()
                );
                case HALF_OPEN -> log.warn(
                        "[CircuitBreaker] 🔄 서킷 HALF-OPEN: {} ({} → {}). " +
                        "복구 여부 확인 중.",
                        name,
                        transition.getFromState(),
                        transition.getToState()
                );
                case CLOSED -> log.info(
                        "[CircuitBreaker] ✅ 서킷 CLOSED: {} ({} → {}). " +
                        "서비스 정상 복구.",
                        name,
                        transition.getFromState(),
                        transition.getToState()
                );
                default -> log.info(
                        "[CircuitBreaker] 상태 변경: {} ({} → {})",
                        name,
                        transition.getFromState(),
                        transition.getToState()
                );
            }
        });
    }

    // ── 실패율 임계값 초과 이벤트 ────────────────────────────────────────────

    private void registerFailureRateListener(CircuitBreaker cb) {
        cb.getEventPublisher().onFailureRateExceeded(event ->
            log.error("[CircuitBreaker] ❌ 실패율 임계값 초과: {} (실패율={}%)",
                    event.getCircuitBreakerName(),
                    String.format("%.1f", event.getFailureRate()))
        );

        cb.getEventPublisher().onSlowCallRateExceeded(event ->
            log.error("[CircuitBreaker] 🐢 느린 호출 임계값 초과: {} (느린호출율={}%)",
                    event.getCircuitBreakerName(),
                    String.format("%.1f", event.getSlowCallRate()))
        );
    }
}
