package com.commerce.bff.controller;

import com.commerce.bff.dto.OrderRequest;
import com.commerce.bff.dto.OrderResponse;
import com.commerce.bff.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "주문", description = "주문 생성 API (Redis 재고 선점 → Kafka 이벤트 발행)")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

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
}
