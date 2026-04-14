package com.commerce.bff.stock;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 서비스 시작 시 DB → Redis 재고 재적재
 *
 * Redis가 재시작되면 모든 재고 키가 사라지므로,
 * 서비스 기동 시점에 DB 스냅샷을 Redis로 복구한다.
 * 이미 Redis에 키가 존재하면 덮어쓰지 않는다 (정상 운영 중 재시작 보호).
 */
@Component
@RequiredArgsConstructor
public class StockRedisLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StockRedisLoader.class);
    private static final String STOCK_KEY_PREFIX = "stock:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductStockRepository productStockRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<ProductStock> stocks = productStockRepository.findAll();
        if (stocks.isEmpty()) {
            log.info("[StockLoader] DB에 재고 데이터 없음. 스킵.");
            return;
        }

        int loaded = 0;
        for (ProductStock stock : stocks) {
            String key = STOCK_KEY_PREFIX + stock.getProductId();
            // Redis에 이미 값이 있으면 덮어쓰지 않음 (운영 중 재시작 시 현재값 보호)
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(stock.getQuantity()));
            if (Boolean.TRUE.equals(absent)) {
                loaded++;
                log.debug("[StockLoader] Loaded. productId={}, quantity={}", stock.getProductId(), stock.getQuantity());
            }
        }

        log.info("[StockLoader] Redis 재고 재적재 완료. 전체={}, 신규 적재={}", stocks.size(), loaded);
    }
}
