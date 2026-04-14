package com.commerce.bff.stock;

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
    @Transactional
    public void initStock(String productId, long quantity) {
        String key = stockKey(productId);
        redisTemplate.opsForValue().set(key, String.valueOf(quantity));

        // DB write-through: 초기화 시점은 항상 정확한 값을 DB에 기록
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
     * @return true: 선점 성공 / false: 재고 부족
     */
    public boolean decreaseStock(List<StockDecreaseItem> items) {
        // 1단계: 모든 상품 재고 차감 시도
        for (StockDecreaseItem item : items) {
            Long remaining = redisTemplate.opsForValue().decrement(stockKey(item.productId()), item.quantity());

            if (remaining == null || remaining < 0) {
                // 재고 부족 → 지금까지 차감한 것 전부 롤백
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
    public void restoreStock(String productId, long quantity) {
        Long after = redisTemplate.opsForValue().increment(stockKey(productId), quantity);
        log.info("[Stock] Restored. productId={}, quantity={}, totalAfter={}", productId, quantity, after);
    }

    /**
     * 현재 재고 조회
     */
    public long getStock(String productId) {
        String value = redisTemplate.opsForValue().get(stockKey(productId));
        if (value == null) return -1L; // 키 없음 (미초기화)
        return Long.parseLong(value);
    }

    // ── private ──────────────────────────────────────────────────────────────

    private void rollback(List<StockDecreaseItem> items, String failedProductId) {
        for (StockDecreaseItem item : items) {
            if (item.productId().equals(failedProductId)) break; // 실패 지점까지만 롤백
            redisTemplate.opsForValue().increment(stockKey(item.productId()), item.quantity());
            log.info("[Stock] Rolled back. productId={}, quantity={}", item.productId(), item.quantity());
        }
    }

    private String stockKey(String productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    /**
     * 재고 차감 요청 단위
     */
    public record StockDecreaseItem(String productId, long quantity) {}
}
