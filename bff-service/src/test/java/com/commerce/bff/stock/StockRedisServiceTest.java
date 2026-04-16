package com.commerce.bff.stock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockRedisService 단위 테스트")
class StockRedisServiceTest {

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @InjectMocks
    StockRedisService stockRedisService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ── decreaseStock ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("decreaseStock")
    class DecreaseStockTest {

        @Test
        @DisplayName("단건 상품 재고가 충분하면 true 반환")
        void singleItem_sufficientStock_returnsTrue() {
            when(valueOperations.decrement("stock:product-A", 2L)).thenReturn(8L);

            boolean result = stockRedisService.decreaseStock(
                    List.of(new StockRedisService.StockDecreaseItem("product-A", 2)));

            assertThat(result).isTrue();
            verify(valueOperations).decrement("stock:product-A", 2L);
        }

        @Test
        @DisplayName("재고 부족(remaining < 0) 시 false 반환 + 롤백 없음(이미 0개이므로)")
        void singleItem_insufficientStock_returnsFalse_noRollback() {
            when(valueOperations.decrement("stock:product-A", 5L)).thenReturn(-1L);

            boolean result = stockRedisService.decreaseStock(
                    List.of(new StockRedisService.StockDecreaseItem("product-A", 5)));

            assertThat(result).isFalse();
            // 첫 번째 아이템 실패 → 이전 성공 항목 없으므로 increment 호출 안 됨
            verify(valueOperations, never()).increment(anyString(), anyLong());
        }

        @Test
        @DisplayName("두 번째 상품 재고 부족 시 첫 번째 상품 재고만 롤백")
        void secondItemFails_rollbackFirstItemOnly() {
            when(valueOperations.decrement("stock:product-A", 1L)).thenReturn(9L);  // 성공
            when(valueOperations.decrement("stock:product-B", 10L)).thenReturn(-1L); // 실패

            boolean result = stockRedisService.decreaseStock(List.of(
                    new StockRedisService.StockDecreaseItem("product-A", 1),
                    new StockRedisService.StockDecreaseItem("product-B", 10)
            ));

            assertThat(result).isFalse();
            // product-A는 롤백되어야 함
            verify(valueOperations).increment("stock:product-A", 1L);
            // product-B는 차감도 못 했으므로 rollback 없음
            verify(valueOperations, never()).increment(eq("stock:product-B"), anyLong());
        }

        @Test
        @DisplayName("세 번째 상품 재고 부족 시 첫 번째, 두 번째만 롤백")
        void thirdItemFails_rollbackFirstAndSecondOnly() {
            when(valueOperations.decrement("stock:product-A", 2L)).thenReturn(8L);
            when(valueOperations.decrement("stock:product-B", 3L)).thenReturn(7L);
            when(valueOperations.decrement("stock:product-C", 99L)).thenReturn(-1L);

            boolean result = stockRedisService.decreaseStock(List.of(
                    new StockRedisService.StockDecreaseItem("product-A", 2),
                    new StockRedisService.StockDecreaseItem("product-B", 3),
                    new StockRedisService.StockDecreaseItem("product-C", 99)
            ));

            assertThat(result).isFalse();
            verify(valueOperations).increment("stock:product-A", 2L);
            verify(valueOperations).increment("stock:product-B", 3L);
            verify(valueOperations, never()).increment(eq("stock:product-C"), anyLong());
        }

        @Test
        @DisplayName("remaining이 null이면 false 반환")
        void nullRemaining_returnsFalse() {
            when(valueOperations.decrement("stock:product-A", 1L)).thenReturn(null);

            boolean result = stockRedisService.decreaseStock(
                    List.of(new StockRedisService.StockDecreaseItem("product-A", 1)));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("remaining이 정확히 0이면 성공 (마지막 재고 선점)")
        void remainingZero_returnsTrue() {
            when(valueOperations.decrement("stock:product-A", 3L)).thenReturn(0L);

            boolean result = stockRedisService.decreaseStock(
                    List.of(new StockRedisService.StockDecreaseItem("product-A", 3)));

            assertThat(result).isTrue();
        }
    }

    // ── restoreStock ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("restoreStock")
    class RestoreStockTest {

        @Test
        @DisplayName("재고 복구 시 increment 호출")
        void restoreStock_callsIncrement() {
            when(valueOperations.increment("stock:product-A", 5L)).thenReturn(15L);

            stockRedisService.restoreStock("product-A", 5L);

            verify(valueOperations).increment("stock:product-A", 5L);
        }
    }

    // ── initStock / getStock ──────────────────────────────────────────────────

    @Nested
    @DisplayName("initStock & getStock")
    class InitAndGetStockTest {

        @Test
        @DisplayName("initStock 호출 시 Redis set 실행")
        void initStock_setsValue() {
            stockRedisService.initStock("product-A", 100L);

            verify(valueOperations).set("stock:product-A", "100");
        }

        @Test
        @DisplayName("getStock - 키 존재 시 파싱된 수량 반환")
        void getStock_keyExists_returnsParsedValue() {
            when(valueOperations.get("stock:product-A")).thenReturn("50");

            long stock = stockRedisService.getStock("product-A");

            assertThat(stock).isEqualTo(50L);
        }

        @Test
        @DisplayName("getStock - 키 없으면 -1 반환")
        void getStock_keyNotExists_returnsMinusOne() {
            when(valueOperations.get("stock:product-A")).thenReturn(null);

            long stock = stockRedisService.getStock("product-A");

            assertThat(stock).isEqualTo(-1L);
        }
    }
}
