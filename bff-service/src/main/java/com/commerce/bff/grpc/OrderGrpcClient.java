package com.commerce.bff.grpc;

import com.commerce.grpc.order.GetOrderListRequest;
import com.commerce.grpc.order.GetOrderListResponse;
import com.commerce.grpc.order.GetOrderStatusRequest;
import com.commerce.grpc.order.GetOrderStatusResponse;
import com.commerce.grpc.order.OrderQueryServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * order-service gRPC 클라이언트
 *
 * @GrpcClient("order-service") → application.yml의 grpc.client.order-service 설정 사용
 * @CircuitBreaker → 연속 실패 시 서킷 오픈, fallback 반환
 */
@Component
public class OrderGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(OrderGrpcClient.class);

    @GrpcClient("order-service")
    private OrderQueryServiceGrpc.OrderQueryServiceBlockingStub orderStub;

    @CircuitBreaker(name = "order-service", fallbackMethod = "getOrderStatusFallback")
    public GetOrderStatusResponse getOrderStatus(String orderId, String userId) {
        log.debug("[gRPC-Client] getOrderStatus. orderId={}", orderId);
        return orderStub.getOrderStatus(GetOrderStatusRequest.newBuilder()
                .setOrderId(orderId)
                .setUserId(userId)
                .build());
    }

    @CircuitBreaker(name = "order-service", fallbackMethod = "getOrderListFallback")
    public GetOrderListResponse getOrderList(String userId) {
        log.debug("[gRPC-Client] getOrderList. userId={}", userId);
        return orderStub.getOrderList(GetOrderListRequest.newBuilder()
                .setUserId(userId)
                .build());
    }

    // ── Fallback Methods ──────────────────────────────────────────────────────
    // proto3 기본값: found=false, 문자열="", repeated=빈 리스트
    // → found=false 이면 Controller에서 서비스 장애로 처리

    public GetOrderStatusResponse getOrderStatusFallback(String orderId, String userId, Throwable t) {
        log.warn("[CircuitBreaker] order-service getOrderStatus fallback. orderId={}, reason={}", orderId, t.getMessage());
        return GetOrderStatusResponse.newBuilder()
                .setFound(false)
                .build();
    }

    public GetOrderListResponse getOrderListFallback(String userId, Throwable t) {
        log.warn("[CircuitBreaker] order-service getOrderList fallback. userId={}, reason={}", userId, t.getMessage());
        return GetOrderListResponse.newBuilder()
                .build(); // orders 빈 리스트
    }
}
