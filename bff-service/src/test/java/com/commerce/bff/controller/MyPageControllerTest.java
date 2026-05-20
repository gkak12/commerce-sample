package com.commerce.bff.controller;

import com.commerce.bff.cache.MyPageCacheService;
import com.commerce.bff.grpc.DeliveryGrpcClient;
import com.commerce.bff.grpc.OrderGrpcClient;
import com.commerce.bff.grpc.PointGrpcClient;
import com.commerce.bff.mapper.MyPageMapperImpl;
import com.commerce.grpc.delivery.GetDeliveryStatusResponse;
import com.commerce.grpc.order.GetOrderListResponse;
import com.commerce.grpc.order.GetOrderStatusResponse;
import com.commerce.grpc.order.OrderSummary;
import com.commerce.grpc.point.GetPointBalanceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MyPageController.class)
@Import({MyPageMapperImpl.class, BaseControllerTest.MethodSecurityConfig.class})
@DisplayName("MyPageController 단위 테스트")
class MyPageControllerTest extends BaseControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    OrderGrpcClient orderGrpcClient;

    @MockBean
    PointGrpcClient pointGrpcClient;

    @MockBean
    DeliveryGrpcClient deliveryGrpcClient;

    // MyPageController 생성자 의존성 (@WebMvcTest는 @Service를 스캔하지 않음)
    @MockBean
    MyPageCacheService cacheService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // 캐시 서비스 pass-through: Supplier를 그대로 실행하여 gRPC mock + 실제 mapper 결과 반영
        // 시그니처: getOrderList(String, int, int, Class<T>, Supplier<T>) → Supplier는 인덱스 4
        lenient().doAnswer(inv -> ((Supplier<?>) inv.getArgument(4)).get())
                .when(cacheService).getOrderList(anyString(), anyInt(), anyInt(), any(), any());

        // 시그니처: getOrderDetail(String, String, Class<T>, Supplier<T>) → Supplier는 인덱스 3
        lenient().doAnswer(inv -> ((Supplier<?>) inv.getArgument(3)).get())
                .when(cacheService).getOrderDetail(anyString(), anyString(), any(), any());

        // 시그니처: getPoints(String, Class<T>, Supplier<T>) → Supplier는 인덱스 2
        lenient().doAnswer(inv -> ((Supplier<?>) inv.getArgument(2)).get())
                .when(cacheService).getPoints(anyString(), any(), any());
    }

    // ── GET /api/my/orders ────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user-001")
    @DisplayName("내 주문 목록 조회 - 2건 반환")
    void getMyOrders_success() throws Exception {
        GetOrderListResponse grpcResp = GetOrderListResponse.newBuilder()
                .addOrders(OrderSummary.newBuilder()
                        .setOrderId("order-1")
                        .setStatus("CONFIRMED")
                        .setTotalAmount("10000")
                        .setCreatedAt("2024-01-01T10:00:00")
                        .build())
                .addOrders(OrderSummary.newBuilder()
                        .setOrderId("order-2")
                        .setStatus("DELIVERING")
                        .setTotalAmount("25000")
                        .setCreatedAt("2024-01-02T11:00:00")
                        .build())
                .setTotalCount(2)
                .setTotalPages(1)
                .setCurrentPage(0)
                .build();

        when(orderGrpcClient.getOrderList("user-001", 0, 10)).thenReturn(grpcResp);

        mockMvc.perform(get("/api/my/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-001"))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.orders[0].orderId").value("order-1"))
                .andExpect(jsonPath("$.orders[1].status").value("DELIVERING"));
    }

    @Test
    @WithMockUser(username = "user-001")
    @DisplayName("내 주문 목록 - 주문 없으면 빈 배열 반환")
    void getMyOrders_empty() throws Exception {
        when(orderGrpcClient.getOrderList("user-001", 0, 10))
                .thenReturn(GetOrderListResponse.newBuilder().build());

        mockMvc.perform(get("/api/my/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.orders").isEmpty());
    }

    // ── GET /api/my/orders/{orderId} ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "user-001")
    @DisplayName("주문 상세 조회 - 주문 + 배송 정보 포함")
    void getOrderDetail_found_withDelivery() throws Exception {
        GetOrderStatusResponse orderResp = GetOrderStatusResponse.newBuilder()
                .setFound(true)
                .setOrderId("order-1")
                .setStatus("DELIVERING")
                .setTotalAmount("30000")
                .setCreatedAt("2024-01-01T10:00:00")
                .build();

        GetDeliveryStatusResponse deliveryResp = GetDeliveryStatusResponse.newBuilder()
                .setFound(true)
                .setDeliveryId("delivery-1")
                .setStatus("IN_TRANSIT")
                .setAddress("서울시 강남구")
                .setStartedAt("2024-01-02T09:00:00")
                .build();

        when(orderGrpcClient.getOrderStatus("order-1", "user-001")).thenReturn(orderResp);
        when(deliveryGrpcClient.getDeliveryStatus("order-1", "user-001")).thenReturn(deliveryResp);

        mockMvc.perform(get("/api/my/orders/order-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.orderId").value("order-1"))
                .andExpect(jsonPath("$.status").value("DELIVERING"))
                .andExpect(jsonPath("$.delivery.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.delivery.address").value("서울시 강남구"));
    }

    @Test
    @WithMockUser(username = "user-001")
    @DisplayName("주문 상세 조회 - 배송 시작 전이면 delivery 필드 없음")
    void getOrderDetail_found_noDelivery() throws Exception {
        GetOrderStatusResponse orderResp = GetOrderStatusResponse.newBuilder()
                .setFound(true)
                .setOrderId("order-1")
                .setStatus("CONFIRMED")
                .setTotalAmount("10000")
                .setCreatedAt("2024-01-01T10:00:00")
                .build();

        when(orderGrpcClient.getOrderStatus("order-1", "user-001")).thenReturn(orderResp);
        when(deliveryGrpcClient.getDeliveryStatus("order-1", "user-001"))
                .thenReturn(GetDeliveryStatusResponse.newBuilder().setFound(false).build());

        mockMvc.perform(get("/api/my/orders/order-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.delivery").doesNotExist());
    }

    @Test
    @WithMockUser(username = "user-001")
    @DisplayName("주문 상세 조회 - 존재하지 않는 주문")
    void getOrderDetail_notFound() throws Exception {
        when(orderGrpcClient.getOrderStatus("order-999", "user-001"))
                .thenReturn(GetOrderStatusResponse.newBuilder().setFound(false).build());

        mockMvc.perform(get("/api/my/orders/order-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.message").value("주문을 찾을 수 없습니다."));

        verifyNoInteractions(deliveryGrpcClient);
    }

    // ── GET /api/my/points ────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user-001")
    @DisplayName("포인트 잔액 조회 - 정상")
    void getMyPoints_success() throws Exception {
        when(pointGrpcClient.getPointBalance("user-001"))
                .thenReturn(GetPointBalanceResponse.newBuilder()
                        .setFound(true)
                        .setUserId("user-001")
                        .setTotalPoint(3500L)
                        .build());

        mockMvc.perform(get("/api/my/points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoint").value(3500))
                .andExpect(jsonPath("$.found").value(true));
    }

    @Test
    @WithMockUser(username = "new-user")
    @DisplayName("포인트 지갑 미생성 사용자 - totalPoint=0")
    void getMyPoints_noWallet() throws Exception {
        when(pointGrpcClient.getPointBalance("new-user"))
                .thenReturn(GetPointBalanceResponse.newBuilder()
                        .setFound(false)
                        .setTotalPoint(0L)
                        .build());

        mockMvc.perform(get("/api/my/points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoint").value(0))
                .andExpect(jsonPath("$.found").value(false));
    }
}
