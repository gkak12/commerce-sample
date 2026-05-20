package com.commerce.bff.controller;

import com.commerce.bff.cache.MyPageCacheService;
import com.commerce.bff.dto.mypage.OrderDetailResponse;
import com.commerce.bff.dto.mypage.OrderListResponse;
import com.commerce.bff.dto.mypage.PointResponse;
import com.commerce.bff.grpc.DeliveryGrpcClient;
import com.commerce.bff.grpc.OrderGrpcClient;
import com.commerce.bff.grpc.PointGrpcClient;
import com.commerce.bff.mapper.MyPageMapper;
import com.commerce.grpc.delivery.GetDeliveryStatusResponse;
import com.commerce.grpc.order.GetOrderStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마이페이지 API — 내부 서비스 조회는 gRPC 로 처리
 *
 * GET /api/my/orders           → order-service (gRPC) : 주문 목록
 * GET /api/my/orders/{orderId} → order-service + delivery-service (gRPC) : 주문 상세 + 배송 상태
 * GET /api/my/points           → point-service (gRPC) : 포인트 잔액
 *
 * 캐싱 전략:
 *   - Cache-Aside + Mutex Lock: TTL 만료 시 Cache Stampede 방어
 *   - 이벤트 기반 무효화: order.completed / order.cancelled 수신 시 즉시 evict
 */
@Tag(name = "마이페이지", description = "주문/배송/포인트 조회 (내부 gRPC 통신)")
@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyPageController {

    private final OrderGrpcClient orderGrpcClient;
    private final PointGrpcClient pointGrpcClient;
    private final DeliveryGrpcClient deliveryGrpcClient;
    private final MyPageCacheService cacheService;
    private final MyPageMapper myPageMapper;

    // ── 내 주문 목록 ────────────────────────────────────────────────────────────
    @Operation(summary = "내 주문 목록", description = "order-service에 gRPC로 조회합니다. (Redis 캐싱 30초)")
    @GetMapping("/orders")
    public ResponseEntity<OrderListResponse> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        String userId = userDetails.getUsername();

        OrderListResponse result = cacheService.getOrderList(userId, page, size,
                OrderListResponse.class,
                () -> myPageMapper.toOrderListResponse(userId,
                        orderGrpcClient.getOrderList(userId, page, size)));

        return ResponseEntity.ok(result);
    }

    // ── 주문 상세 + 배송 상태 통합 조회 ────────────────────────────────────────
    @Operation(summary = "주문 상세 + 배송 상태", description = "order-service와 delivery-service에 gRPC로 동시 조회합니다. (Redis 캐싱 10초)")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
            @PathVariable String orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();

        OrderDetailResponse result = cacheService.getOrderDetail(userId, orderId,
                OrderDetailResponse.class,
                () -> {
                    // order-service gRPC 호출
                    GetOrderStatusResponse orderResp = orderGrpcClient.getOrderStatus(orderId, userId);
                    if (!orderResp.getFound()) {
                        return myPageMapper.toOrderDetailNotFound();
                    }

                    // delivery-service gRPC 호출 (배송 시작 전이면 found=false)
                    GetDeliveryStatusResponse deliveryResp =
                            deliveryGrpcClient.getDeliveryStatus(orderId, userId);

                    return myPageMapper.toOrderDetailResponse(orderResp, deliveryResp);
                });

        return ResponseEntity.ok(result);
    }

    // ── 포인트 잔액 조회 ────────────────────────────────────────────────────────
    @Operation(summary = "포인트 잔액", description = "point-service에 gRPC로 조회합니다. (Redis 캐싱 60초)")
    @GetMapping("/points")
    public ResponseEntity<PointResponse> getMyPoints(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();

        PointResponse result = cacheService.getPoints(userId, PointResponse.class,
                () -> myPageMapper.toPointResponse(userId,
                        pointGrpcClient.getPointBalance(userId)));

        return ResponseEntity.ok(result);
    }
}
