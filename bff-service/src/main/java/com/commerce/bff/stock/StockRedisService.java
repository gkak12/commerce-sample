package com.commerce.bff.stock;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Redis 기반 재고 선점 서비스
 *
 * 키 설계: stock:{productId}  →  value: 남은 재고 수량 (Long)
 *
 * DECR 명령어는 원자적(Atomic) 연산이므로
 * 동시에 100명이 요청해도 정확히 재고 수량만큼만 선점됨.
 *
 * Redis 장애 시:
 *   - decreaseStock : false 반환 (재고 부족으로 처리 → 과재고 방지 우선)
 *   - restoreStock  : 경고 로그만 (복구는 스냅샷 스케줄러가 처리)
 *   - getStock      : DB에서 직접 조회 (fallback)
 *   - initStock     : 에러 로그 (관리자 재시도 필요)
 */
@Service
@RequiredArgsConstructor
public class StockRedisService {

    private static final Logger log = LoggerFactory.getLogger(StockRedisService.class);
    private static final String STOCK_KEY_PREFIX = "stock:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductStockRepository productStockRepository;

    /**
     * 재고 초기화 (상품 등록 시 호출)
     * Write-through: Redis + DB 동시 기록 (관리자 작업, 빈도 낮음)
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "initStockFallback")
    @Retry(name = "redis")
    @Transactional
    public void initStock(String productId, long quantity) {
        String key = stockKey(productId);
        redisTemplate.opsForValue().set(key, String.valueOf(quantity));

        productStockRepository.findById(productId)
                .ifPresentOrElse(
                        stock -> stock.update(quantity, LocalDateTime.now()),
                        () -> productStockRepository.save(new ProductStock(productId, quantity, LocalDateTime.now()))
                );

        log.info("[Stock] Initialized. productId={}, quantity={}", productId, quantity);
    }

    /**
     * 다건 상품 재고 원자적 선점
     * 하나라도 재고 부족이면 이미 차감된 재고 전부 롤백 후 false 반환
     *
     * @return true: 선점 성공 / false: 재고 부족 또는 Redis 장애
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "decreaseStockFallback")
    @Retry(name = "redis")
    public boolean decreaseStock(List<StockDecreaseItem> items) {
        for (StockDecreaseItem item : items) {
            Long remaining = redisTemplate.opsForValue().decrement(stockKey(item.productId()), item.quantity());

            if (remaining == null || remaining < 0) {
                log.warn("[Stock] Out of stock. productId={}, remaining={}", item.productId(), remaining);
                rollback(items, item.productId());
                return false;
            }
            log.debug("[Stock] Decreased. productId={}, quantity={}, remaining={}",
                    item.productId(), item.quantity(), remaining);
        }
        return true;
    }

    /**
     * 재고 복구 (주문 취소 / 결제 실패 시 Saga 보상으로 호출)
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "restoreStockFallback")
    @Retry(name = "redis")
    public void restoreStock(String productId, long quantity) {
        Long after = redisTemplate.opsForValue().increment(stockKey(productId), quantity);
        log.info("[Stock] Restored. productId={}, quantity={}, totalAfter={}", productId, quantity, after);
    }

    /**
     * 현재 재고 조회
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "getStockFallback")
    @Retry(name = "redis")
    public long getStock(String productId) {
        String value = redisTemplate.opsForValue().get(stockKey(productId));
        if (value == null) return -1L;
        return Long.parseLong(value);
    }

    // ── Fallback Methods ──────────────────────────────────────────────────────

    public void initStockFallback(String productId, long quantity, Throwable t) {
        log.error("[CircuitBreaker] Redis initStock fallback. productId={}, reason={}", productId, t.getMessage());
        // DB에는 정상 기록 (Redis만 실패)
        productStockRepository.findById(productId)
                .ifPresentOrElse(
                        stock -> stock.update(quantity, LocalDateTime.now()),
                        () -> productStockRepository.save(new ProductStock(productId, quantity, LocalDateTime.now()))
                );
    }

    public boolean decreaseStockFallback(List<StockDecreaseItem> items, Throwable t) {
        log.error("[CircuitBreaker] Redis decreaseStock fallback. reason={} → 재고 부족으로 처리 (과재고 방지)", t.getMessage());
        // Redis 장애 시 false 반환 → 주문 실패 처리 (과재고보다 안전)
        return false;
    }

    public void restoreStockFallback(String productId, long quantity, Throwable t) {
        log.error("[CircuitBreaker] Redis restoreStock fallback. productId={}, reason={} → 스냅샷 스케줄러가 보정 예정", productId, t.getMessage());
        // 스냅샷 스케줄러가 DB → Redis 동기화 시 자동 보정됨
    }

    public long getStockFallback(String productId, Throwable t) {
        log.warn("[CircuitBreaker] Redis getStock fallback. productId={}, reason={} → DB 조회", productId, t.getMessage());
        // Redis 대신 DB에서 직접 조회
        return productStockRepository.findById(productId)
                .map(ProductStock::getQuantity)
                .orElse(-1L);
    }

    // ── private ──────────────────────────────────────────────────────────────

    private void rollback(List<StockDecreaseItem> items, String failedProductId) {
        for (StockDecreaseItem item : items) {
            if (item.productId().equals(failedProductId)) break;
            redisTemplate.opsForValue().increment(stockKey(item.productId()), item.quantity());
            log.info("[Stock] Rolled back. productId={}, quantity={}", item.productId(), item.quantity());
        }
    }

    private String stockKey(String productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    public record StockDecreaseItem(String productId, long quantity) {}
}
