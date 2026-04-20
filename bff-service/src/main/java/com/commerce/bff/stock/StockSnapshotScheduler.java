package com.commerce.bff.stock;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis 재고 → DB 주기적 스냅샷 동기화
 *
 * - Redis 장애/재시작 시 DB에서 재고 복구 가능하도록 주기적으로 현재값을 DB에 upsert
 * - 이벤트별 write 대신 주기 단위로 묶어서 DB 부하 최소화
 * - 기본 주기: 30초 (stock.snapshot.interval-ms 로 조정 가능)
 */
@Component
@RequiredArgsConstructor
public class StockSnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockSnapshotScheduler.class);
    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String STOCK_KEY_PATTERN = "stock:*";

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductStockRepository productStockRepository;

    @Scheduled(fixedDelayString = "${stock.snapshot.interval-ms:30000}")
    @SchedulerLock(name = "stock_snapshot", lockAtMostFor = "PT45S", lockAtLeastFor = "PT25S")
    @Transactional
    public void syncToDb() {
        List<ProductStock> snapshots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        ScanOptions options = ScanOptions.scanOptions()
                .match(STOCK_KEY_PATTERN)
                .count(100)
                .build();

        // KEYS 대신 SCAN 사용 (Redis 블로킹 방지)
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String value = redisTemplate.opsForValue().get(key);
                if (value == null) continue;

                String productId = key.substring(STOCK_KEY_PREFIX.length());
                long quantity = Long.parseLong(value);

                snapshots.add(productStockRepository.findById(productId)
                        .map(existing -> { existing.update(quantity, now); return existing; })
                        .orElse(new ProductStock(productId, quantity, now)));
            }
        } catch (Exception e) {
            log.error("[StockSnapshot] Redis scan 중 오류 발생", e);
            return;
        }

        if (snapshots.isEmpty()) {
            log.debug("[StockSnapshot] 동기화할 재고 없음");
            return;
        }

        productStockRepository.saveAll(snapshots);
        log.info("[StockSnapshot] DB 동기화 완료. 상품 수={}, syncedAt={}", snapshots.size(), now);
    }
}
