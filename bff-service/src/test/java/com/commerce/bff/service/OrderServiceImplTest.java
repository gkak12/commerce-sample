package com.commerce.bff.service;

import com.commerce.bff.dto.OrderItemRequest;
import com.commerce.bff.dto.OrderRequest;
import com.commerce.bff.dto.OrderResponse;
import com.commerce.bff.kafka.OrderEventProducer;
import com.commerce.bff.service.impl.OrderServiceImpl;
import com.commerce.bff.stock.StockRedisService;
import com.commerce.common.event.OrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl 단위 테스트")
class OrderServiceImplTest {

    @Mock
    OrderEventProducer orderEventProducer;

    @Mock
    StockRedisService stockRedisService;

    @InjectMocks
    OrderServiceImpl orderService;

    // ── 정상 주문 흐름 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("재고 선점 성공 + Kafka 발행 → 주문 접수 메시지 반환")
    void placeOrder_success() {
        // given
        when(stockRedisService.decreaseStock(any())).thenReturn(true);

        OrderRequest request = buildRequest("user-1", "product-A", 2, new BigDecimal("5000"));

        // when
        OrderResponse response = orderService.placeOrder(request);

        // then
        assertThat(response.getMessage()).isEqualTo("주문이 접수되었습니다.");
        assertThat(response.getOrderId()).isNotBlank();
        verify(stockRedisService).decreaseStock(any());
        verify(orderEventProducer).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("Kafka 이벤트에 올바른 totalAmount 포함 여부 검증")
    void placeOrder_totalAmountCalculation() {
        // given — 상품 2개, 각 수량 x 단가 합산 검증
        when(stockRedisService.decreaseStock(any())).thenReturn(true);

        OrderRequest request = OrderRequest.builder()
                .userId("user-1")
                .items(List.of(
                        buildItem("product-A", 2, new BigDecimal("3000")),  // 6,000
                        buildItem("product-B", 3, new BigDecimal("1000"))   // 3,000
                ))
                .build();

        // when
        orderService.placeOrder(request);

        // then — publishOrderCreated 에 전달된 이벤트의 totalAmount = 9,000
        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventProducer).publishOrderCreated(captor.capture());
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(new BigDecimal("9000"));
    }

    // ── 재고 부족 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("재고 부족 시 '재고가 부족합니다.' 반환 + Kafka 미발행")
    void placeOrder_stockUnavailable() {
        // given
        when(stockRedisService.decreaseStock(any())).thenReturn(false);

        OrderRequest request = buildRequest("user-1", "product-A", 100, new BigDecimal("1000"));

        // when
        OrderResponse response = orderService.placeOrder(request);

        // then
        assertThat(response.getMessage()).isEqualTo("재고가 부족합니다.");
        verifyNoInteractions(orderEventProducer);
    }

    // ── Kafka 발행 실패 시 재고 롤백 ─────────────────────────────────────────

    @Test
    @DisplayName("Kafka 발행 실패 시 선점한 재고 복구 후 예외 전파")
    void placeOrder_kafkaFails_rollbackStock() {
        // given
        when(stockRedisService.decreaseStock(any())).thenReturn(true);
        doThrow(new RuntimeException("Kafka 연결 실패"))
                .when(orderEventProducer).publishOrderCreated(any());

        OrderRequest request = buildRequest("user-1", "product-A", 2, new BigDecimal("5000"));

        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("주문 처리 중 오류");

        // 재고 복구 호출 여부 검증 (product-A, quantity=2)
        verify(stockRedisService).restoreStock("product-A", 2);
    }

    @Test
    @DisplayName("Kafka 실패 시 모든 아이템 재고 복구")
    void placeOrder_kafkaFails_rollbackAllItems() {
        // given
        when(stockRedisService.decreaseStock(any())).thenReturn(true);
        doThrow(new RuntimeException("Kafka down")).when(orderEventProducer).publishOrderCreated(any());

        OrderRequest request = OrderRequest.builder()
                .userId("user-1")
                .items(List.of(
                        buildItem("product-A", 2, new BigDecimal("3000")),
                        buildItem("product-B", 1, new BigDecimal("5000"))
                ))
                .build();

        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(RuntimeException.class);

        verify(stockRedisService).restoreStock("product-A", 2);
        verify(stockRedisService).restoreStock("product-B", 1);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private OrderRequest buildRequest(String userId, String productId, int qty, BigDecimal price) {
        return OrderRequest.builder()
                .userId(userId)
                .items(List.of(buildItem(productId, qty, price)))
                .build();
    }

    private OrderItemRequest buildItem(String productId, int qty, BigDecimal price) {
        return OrderItemRequest.builder()
                .productId(productId)
                .productName("상품-" + productId)
                .quantity(qty)
                .price(price)
                .build();
    }
}
