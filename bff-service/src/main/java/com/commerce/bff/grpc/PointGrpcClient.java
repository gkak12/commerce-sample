package com.commerce.bff.grpc;

import com.commerce.grpc.point.GetPointBalanceRequest;
import com.commerce.grpc.point.GetPointBalanceResponse;
import com.commerce.grpc.point.PointQueryServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * point-service gRPC 클라이언트
 *
 * @CircuitBreaker → 연속 실패 시 서킷 오픈, fallback 반환
 */
@Component
public class PointGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(PointGrpcClient.class);

    @GrpcClient("point-service")
    private PointQueryServiceGrpc.PointQueryServiceBlockingStub pointStub;

    @CircuitBreaker(name = "point-service", fallbackMethod = "getPointBalanceFallback")
    public GetPointBalanceResponse getPointBalance(String userId) {
        log.debug("[gRPC-Client] getPointBalance. userId={}", userId);
        return pointStub.getPointBalance(GetPointBalanceRequest.newBuilder()
                .setUserId(userId)
                .build());
    }

    // ── Fallback Methods ──────────────────────────────────────────────────────
    // proto3 기본값: found=false, total_point=0
    // → found=false 이면 Controller에서 서비스 장애로 처리

    public GetPointBalanceResponse getPointBalanceFallback(String userId, Throwable t) {
        log.warn("[CircuitBreaker] point-service getPointBalance fallback. userId={}, reason={}", userId, t.getMessage());
        return GetPointBalanceResponse.newBuilder()
                .setFound(false)
                .build();
    }
}
