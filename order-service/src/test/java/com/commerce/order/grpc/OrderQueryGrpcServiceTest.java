package com.commerce.order.grpc;

import com.commerce.grpc.order.GetOrderListRequest;
import com.commerce.grpc.order.GetOrderListResponse;
import com.commerce.grpc.order.GetOrderStatusRequest;
import com.commerce.grpc.order.GetOrderStatusResponse;
import com.commerce.grpc.order.OrderSummary;
import com.commerce.order.entity.Order;
import com.commerce.order.entity.OrderItem;
import com.commerce.order.entity.OrderStatus;
import com.commerce.order.repository.OrderRepository;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderQueryGrpcService 단위 테스트")
class OrderQueryGrpcServiceTest {

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OrderQueryGrpcService grpcService;

    // ── getOrderStatus ────────────────────────────────────────────────────────

    @Test
    @DisplayName("orderId와 userId 일치 → found=true, 올바른 상태 반환")
    void getOrderStatus_found() {
        // given
        Order order = Order.builder()
                .orderId("order-1")
                .userId("user-1")
                .totalAmount(new BigDecimal("15000"))
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        StreamObserver<GetOrderStatusResponse> observer = mock(StreamObserver.class);
        ArgumentCaptor<GetOrderStatusResponse> captor = ArgumentCaptor.forClass(GetOrderStatusResponse.class);

        // when
        grpcService.getOrderStatus(GetOrderStatusRequest.newBuilder()
                .setOrderId("order-1")
                .setUserId("user-1")
                .build(), observer);

        // then
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        verify(observer, never()).onError(any());

        GetOrderStatusResponse resp = captor.getValue();
        assertThat(resp.getFound()).isTrue();
        assertThat(resp.getOrderId()).isEqualTo("order-1");
        assertThat(resp.getStatus()).isEqualTo("CONFIRMED");
        assertThat(resp.getTotalAmount()).isEqualTo("15000");
    }

    @Test
    @DisplayName("존재하지 않는 orderId → found=false")
    void getOrderStatus_orderNotFound() {
        when(orderRepository.findById("order-999")).thenReturn(Optional.empty());

        StreamObserver<GetOrderStatusResponse> observer = mock(StreamObserver.class);
        ArgumentCaptor<GetOrderStatusResponse> captor = ArgumentCaptor.forClass(GetOrderStatusResponse.class);

        grpcService.getOrderStatus(GetOrderStatusRequest.newBuilder()
                .setOrderId("order-999")
                .setUserId("user-1")
                .build(), observer);

        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        assertThat(captor.getValue().getFound()).isFalse();
    }

    @Test
    @DisplayName("userId 불일치(타인 주문 접근) → found=false")
    void getOrderStatus_userMismatch_returnsFalse() {
        Order order = Order.builder()
                .orderId("order-1")
                .userId("user-1")           // 실제 소유자
                .totalAmount(BigDecimal.TEN)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        StreamObserver<GetOrderStatusResponse> observer = mock(StreamObserver.class);
        ArgumentCaptor<GetOrderStatusResponse> captor = ArgumentCaptor.forClass(GetOrderStatusResponse.class);

        grpcService.getOrderStatus(GetOrderStatusRequest.newBuilder()
                .setOrderId("order-1")
                .setUserId("user-99")       // 다른 사용자
                .build(), observer);

        verify(observer).onNext(captor.capture());
        assertThat(captor.getValue().getFound()).isFalse();
    }

    @Test
    @DisplayName("주문 상세에 OrderItem 목록 포함 여부 검증")
    void getOrderStatus_includesOrderItems() {
        Order order = Order.builder()
                .orderId("order-1")
                .userId("user-1")
                .totalAmount(new BigDecimal("20000"))
                .status(OrderStatus.CONFIRMED)
                .build();

        order.addItem("prod-A", "상품A", 2, new BigDecimal("10000"));

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        StreamObserver<GetOrderStatusResponse> observer = mock(StreamObserver.class);
        ArgumentCaptor<GetOrderStatusResponse> captor = ArgumentCaptor.forClass(GetOrderStatusResponse.class);

        grpcService.getOrderStatus(GetOrderStatusRequest.newBuilder()
                .setOrderId("order-1").setUserId("user-1").build(), observer);

        verify(observer).onNext(captor.capture());
        GetOrderStatusResponse resp = captor.getValue();
        assertThat(resp.getItemsCount()).isEqualTo(1);
        assertThat(resp.getItems(0).getProductId()).isEqualTo("prod-A");
        assertThat(resp.getItems(0).getQuantity()).isEqualTo(2);
    }

    // ── getOrderList ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("사용자 주문 목록 조회 - 2건 반환")
    void getOrderList_returnsMultipleOrders() {
        Order order1 = Order.builder().orderId("order-1").userId("user-1")
                .totalAmount(new BigDecimal("10000")).status(OrderStatus.CONFIRMED).build();

        Order order2 = Order.builder().orderId("order-2").userId("user-1")
                .totalAmount(new BigDecimal("25000")).status(OrderStatus.DELIVERING).build();

        List<Order> orders = List.of(order1, order2);

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(eq("user-1"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(orders));

        StreamObserver<GetOrderListResponse> observer = mock(StreamObserver.class);
        ArgumentCaptor<GetOrderListResponse> captor = ArgumentCaptor.forClass(GetOrderListResponse.class);

        grpcService.getOrderList(GetOrderListRequest.newBuilder()
                .setUserId("user-1").build(), observer);

        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();

        GetOrderListResponse resp = captor.getValue();
        assertThat(resp.getOrdersCount()).isEqualTo(2);

        List<OrderSummary> summaries = resp.getOrdersList();
        assertThat(summaries.get(0).getOrderId()).isEqualTo("order-1");
        assertThat(summaries.get(1).getStatus()).isEqualTo("DELIVERING");
    }

    @Test
    @DisplayName("주문 없는 사용자 → 빈 목록 반환")
    void getOrderList_noOrders_returnsEmpty() {
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(eq("user-999"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        StreamObserver<GetOrderListResponse> observer = mock(StreamObserver.class);
        ArgumentCaptor<GetOrderListResponse> captor = ArgumentCaptor.forClass(GetOrderListResponse.class);

        grpcService.getOrderList(GetOrderListRequest.newBuilder()
                .setUserId("user-999").build(), observer);

        verify(observer).onNext(captor.capture());
        assertThat(captor.getValue().getOrdersCount()).isEqualTo(0);
    }
}
