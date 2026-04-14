package com.commerce.bff.controller;

import com.commerce.bff.grpc.DeliveryGrpcClient;
import com.commerce.bff.grpc.OrderGrpcClient;
import com.commerce.bff.grpc.PointGrpcClient;
import com.commerce.grpc.delivery.GetDeliveryStatusResponse;
import com.commerce.grpc.order.GetOrderStatusResponse;
import com.commerce.grpc.order.OrderSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 마이페이지 API — 내부 서비스 조회는 gRPC 로 처리
 *
 * GET /api/my/orders           → order-service (gRPC) : 주문 목록
 * GET /api/my/orders/{orderId} → order-service + delivery-service (gRPC) : 주문 상세 + 배송 상태
 * GET /api/my/points           → point-service (gRPC) : 포인트 잔액
 */
@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyPageController {

    private final OrderGrpcClient orderGrpcClient;
    private final PointGrpcClient pointGrpcClient;
    private final DeliveryGrpcClient deliveryGrpcClient;

    // ── 내 주문 목록 ────────────────────────────────────────────────────────────
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();  // CustomUserDetails.getUsername() = userId

        List<OrderSummary> orders = orderGrpcClient.getOrderList(userId).getOrdersList();

        List<Map<String, Object>> result = orders.stream()
                .map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("orderId", o.getOrderId());
                    m.put("status", o.getStatus());
                    m.put("totalAmount", o.getTotalAmount());
                    m.put("createdAt", o.getCreatedAt());
                    return m;
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "totalCount", result.size(),
                "orders", result
        ));
    }

    // ── 주문 상세 + 배송 상태 통합 조회 ────────────────────────────────────────
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetail(
            @PathVariable String orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();

        // order-service gRPC 호출
        GetOrderStatusResponse orderResp = orderGrpcClient.getOrderStatus(orderId, userId);
        if (!orderResp.getFound()) {
            return ResponseEntity.ok(Map.of("found", false, "message", "주문을 찾을 수 없습니다."));
        }

        // delivery-service gRPC 호출 (배송 시작 전이면 found=false)
        GetDeliveryStatusResponse deliveryResp = deliveryGrpcClient.getDeliveryStatus(orderId, userId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("found", true);
        response.put("orderId", orderResp.getOrderId());
        response.put("status", orderResp.getStatus());
        response.put("totalAmount", orderResp.getTotalAmount());
        response.put("createdAt", orderResp.getCreatedAt());
        response.put("items", orderResp.getItemsList().stream()
                .map(item -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("productId", item.getProductId());
                    m.put("productName", item.getProductName());
                    m.put("quantity", item.getQuantity());
                    m.put("price", item.getPrice());
                    return m;
                })
                .toList());

        if (deliveryResp.getFound()) {
            Map<String, Object> delivery = new LinkedHashMap<>();
            delivery.put("deliveryId", deliveryResp.getDeliveryId());
            delivery.put("status", deliveryResp.getStatus());
            delivery.put("address", deliveryResp.getAddress());
            delivery.put("startedAt", deliveryResp.getStartedAt());
            response.put("delivery", delivery);
        } else {
            response.put("delivery", null);
        }

        return ResponseEntity.ok(response);
    }

    // ── 포인트 잔액 조회 ────────────────────────────────────────────────────────
    @GetMapping("/points")
    public ResponseEntity<Map<String, Object>> getMyPoints(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = userDetails.getUsername();
        var pointResp = pointGrpcClient.getPointBalance(userId);

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "totalPoint", pointResp.getTotalPoint(),
                "found", pointResp.getFound()
        ));
    }
}
