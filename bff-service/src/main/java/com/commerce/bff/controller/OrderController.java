package com.commerce.bff.controller;

import com.commerce.bff.dto.OrderRequest;
import com.commerce.bff.dto.OrderResponse;
import com.commerce.bff.kafka.OrderEventProducer;
import com.commerce.bff.service.OrderService;
import com.commerce.common.event.OrderCancelRequestedEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "주문", description = "주문 생성 API (Redis 재고 선점 → Kafka 이벤트 발행)")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderEventProducer orderEventProducer;

    @Operation(
        summary = "주문 생성",
        description = "Redis에서 재고를 원자적으로 선점한 뒤 Kafka order.created 이벤트를 발행합니다. 재고 부족 시 즉시 실패 반환."
    )
    @ApiResponse(responseCode = "200", description = "주문 접수 성공 또는 재고 부족 메시지")
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "주문 취소", description = "본인 주문을 취소합니다. CONFIRMED 상태인 주문만 취소 가능합니다.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "취소 요청 접수")
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "사용자 요청") String reason,
            @AuthenticationPrincipal UserDetails userDetails) {

        orderEventProducer.publishOrderCancelRequested(
                OrderCancelRequestedEvent.builder()
                        .orderId(orderId)
                        .userId(userDetails.getUsername())
                        .reason(reason)
                        .build()
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "주문 취소 요청이 접수되었습니다.",
                "orderId", orderId
        ));
    }
}
