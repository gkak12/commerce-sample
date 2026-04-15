package com.commerce.bff.grpc;

import com.commerce.grpc.delivery.DeliveryQueryServiceGrpc;
import com.commerce.grpc.delivery.GetDeliveryStatusRequest;
import com.commerce.grpc.delivery.GetDeliveryStatusResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * delivery-service gRPC 클라이언트
 *
 * @CircuitBreaker → 연속 실패 시 서킷 오픈, fallback 반환
 */
@Component
public class DeliveryGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(DeliveryGrpcClient.class);

    @GrpcClient("delivery-service")
    private DeliveryQueryServiceGrpc.DeliveryQueryServiceBlockingStub deliveryStub;

    @CircuitBreaker(name = "delivery-service", fallbackMethod = "getDeliveryStatusFallback")
    public GetDeliveryStatusResponse getDeliveryStatus(String orderId, String userId) {
        log.debug("[gRPC-Client] getDeliveryStatus. orderId={}", orderId);
        return deliveryStub.getDeliveryStatus(GetDeliveryStatusRequest.newBuilder()
                .setOrderId(orderId)
                .setUserId(userId)
                .build());
    }

    // ── Fallback Methods ──────────────────────────────────────────────────────
    // proto3 기본값: found=false, 문자열=""
    // → found=false 이면 Controller에서 서비스 장애로 처리

    public GetDeliveryStatusResponse getDeliveryStatusFallback(String orderId, String userId, Throwable t) {
        log.warn("[CircuitBreaker] delivery-service getDeliveryStatus fallback. orderId={}, reason={}", orderId, t.getMessage());
        return GetDeliveryStatusResponse.newBuilder()
                .setFound(false)
                .build();
    }
}
