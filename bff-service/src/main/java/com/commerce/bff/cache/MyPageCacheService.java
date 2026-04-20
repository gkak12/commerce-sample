package com.commerce.bff.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 마이페이지 조회 캐싱 서비스
 *
 * ┌──────────────────────────────────────────────────────────┐
 * │  Cache-Aside 패턴 + Mutex Lock (Cache Stampede 방어)      │
 * │                                                          │
 * │  1. Redis 캐시 조회 (hit → 즉시 반환)                     │
 * │  2. Cache miss → SETNX로 분산 락 획득 시도               │
 * │     ├─ 락 획득 성공 → double-check → gRPC 호출 → 캐싱   │
 * │     └─ 락 획득 실패 → 50ms 대기 → 캐시 재조회            │
 * │  3. TTL 만료 시 캐시 무효화 → 재조회                      │
 * └──────────────────────────────────────────────────────────┘
 *
 * ┌──────────────────────────────────────────────────────────┐
 * │  이벤트 기반 캐시 무효화                                   │
 * │  order.completed / order.cancelled Kafka 이벤트 수신      │
 * │  → evictOrderCaches() 호출 → 즉시 삭제                   │
 * └──────────────────────────────────────────────────────────┘
 *
 * Cache Key 구조:
 *   cache:orders:{userId}:{page}:{size}
 *   cache:order:{userId}:{orderId}
 *   cache:points:{userId}
 *
 * Lock Key 구조:
 *   lock:cache:orders:{userId}:{page}:{size}
 *   lock:cache:order:{userId}:{orderId}
 *   lock:cache:points:{userId}
 */
@Service
@RequiredArgsConstructor
public class MyPageCacheService {

    private static final Logger log = LoggerFactory.getLogger(MyPageCacheService.class);

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cache.ttl.order-list-seconds:30}")
    private long orderListTtlSeconds;

    @Value("${cache.ttl.order-detail-seconds:10}")
    private long orderDetailTtlSeconds;

    @Value("${cache.ttl.point-seconds:60}")
    private long pointTtlSeconds;

    @Value("${cache.lock.ttl-seconds:3}")
    private long lockTtlSeconds;

    @Value("${cache.lock.wait-ms:50}")
    private long lockWaitMs;

    // ── 주문 목록 ──────────────────────────────────────────────────────────────

    public Map<String, Object> getOrderList(String userId, int page, int size,
                                            Supplier<Map<String, Object>> loader) {
        String cacheKey = "cache:orders:" + userId + ":" + page + ":" + size;
        return getOrLoad(cacheKey, Duration.ofSeconds(orderListTtlSeconds), loader);
    }

    // ── 주문 상세 ──────────────────────────────────────────────────────────────

    public Map<String, Object> getOrderDetail(String userId, String orderId,
                                              Supplier<Map<String, Object>> loader) {
        String cacheKey = "cache:order:" + userId + ":" + orderId;
        return getOrLoad(cacheKey, Duration.ofSeconds(orderDetailTtlSeconds), loader);
    }

    // ── 포인트 잔액 ────────────────────────────────────────────────────────────

    public Map<String, Object> getPoints(String userId, Supplier<Map<String, Object>> loader) {
        String cacheKey = "cache:points:" + userId;
        return getOrLoad(cacheKey, Duration.ofSeconds(pointTtlSeconds), loader);
    }

    // ── 이벤트 기반 캐시 무효화 ────────────────────────────────────────────────

    /**
     * 주문 상태 변경 이벤트(completed / cancelled) 수신 시 관련 캐시 삭제
     *
     * - 주문 상세 캐시: cache:order:{userId}:{orderId} (단건 삭제)
     * - 주문 목록 캐시: cache:orders:{userId}:* (패턴 삭제)
     *   ※ 운영 환경에서 키 수가 많다면 SCAN 방식으로 전환 권장
     *
     * Redis 장애 시: 삭제 실패를 무시하고 계속 진행 (TTL로 자연 만료)
     */
    public void evictOrderCaches(String userId, String orderId) {
        try {
            // 주문 상세 캐시 삭제
            String detailKey = "cache:order:" + userId + ":" + orderId;
            redisTemplate.delete(detailKey);
            log.debug("[Cache][Evict] 주문 상세 캐시 삭제. key={}", detailKey);

            // 주문 목록 캐시 삭제 (페이지/사이즈 조합 전체)
            String listPattern = "cache:orders:" + userId + ":*";
            Set<String> listKeys = redisTemplate.keys(listPattern);
            if (listKeys != null && !listKeys.isEmpty()) {
                redisTemplate.delete(listKeys);
                log.debug("[Cache][Evict] 주문 목록 캐시 삭제. count={}, userId={}", listKeys.size(), userId);
            }
        } catch (DataAccessException e) {
            // 삭제 실패해도 이메일 발송 등 후속 작업에는 영향 없음
            // 캐시는 TTL 만료 시 자연 제거됨
            log.warn("[Cache][Evict] Redis 장애로 캐시 삭제 실패 (TTL 만료로 자연 제거 예정). userId={}, orderId={}, error={}",
                    userId, orderId, e.getMessage());
        }
    }

    // ── 핵심: Mutex Lock 기반 Cache-Aside ─────────────────────────────────────

    /**
     * Cache-Aside + Mutex Lock
     *
     * Cache Stampede 시나리오:
     *   TTL 만료 시점에 N개의 요청이 동시에 cache miss
     *   → SETNX 락을 1개만 획득 → 나머지는 50ms 대기 후 캐시 재조회
     *   → DB/gRPC 호출이 1회로 수렴
     *
     * Redis 장애 시나리오:
     *   RedisException 발생 → 캐시 우회 → gRPC 직접 호출 (graceful degradation)
     *   → 서비스는 정상 응답 유지 (캐시 없이 동작)
     */
    private Map<String, Object> getOrLoad(String cacheKey, Duration ttl,
                                          Supplier<Map<String, Object>> loader) {
        try {
            // 1. 캐시 조회
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("[Cache] HIT. key={}", cacheKey);
                return deserialize(cached);
            }

            log.debug("[Cache] MISS. key={}", cacheKey);

            // 2. Mutex Lock 획득 시도
            String lockKey = "lock:" + cacheKey;
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofSeconds(lockTtlSeconds));

            if (Boolean.TRUE.equals(locked)) {
                try {
                    // 3. Double-check: 락 획득 직전에 다른 스레드가 캐싱했을 수 있음
                    cached = redisTemplate.opsForValue().get(cacheKey);
                    if (cached != null) {
                        log.debug("[Cache] HIT (double-check). key={}", cacheKey);
                        return deserialize(cached);
                    }

                    // 4. 실제 데이터 조회 (gRPC 호출)
                    Map<String, Object> result = loader.get();

                    // 5. 캐싱 (result가 null이면 캐싱하지 않음 — 장애 fallback 결과 제외)
                    if (result != null) {
                        redisTemplate.opsForValue().set(cacheKey, serialize(result), ttl);
                        log.debug("[Cache] SET. key={}, ttl={}s", cacheKey, ttl.getSeconds());
                    }

                    return result;

                } finally {
                    redisTemplate.delete(lockKey);
                }

            } else {
                // 6. 락 획득 실패 → 대기 후 캐시 재조회
                log.debug("[Cache] Lock 대기. key={}", cacheKey);
                try {
                    Thread.sleep(lockWaitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return deserialize(cached);
                }

                // 7. 캐시가 여전히 없으면 직접 호출 (starvation 방지)
                log.warn("[Cache] 락 대기 후에도 캐시 없음. 직접 호출. key={}", cacheKey);
                return loader.get();
            }

        } catch (DataAccessException e) {
            // Redis 장애 → 캐시 완전 우회, gRPC 직접 호출
            log.warn("[Cache][Fallback] Redis 장애로 캐시 우회. key={}, error={}", cacheKey, e.getMessage());
            return loader.get();
        }
    }

    // ── 직렬화 유틸 ────────────────────────────────────────────────────────────

    private String serialize(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("[Cache] Serialization 실패: {}", e.getMessage());
            throw new IllegalStateException("Cache serialization failed", e);
        }
    }

    private Map<String, Object> deserialize(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("[Cache] Deserialization 실패 (캐시 무시). error={}", e.getMessage());
            return null;
        }
    }
}
