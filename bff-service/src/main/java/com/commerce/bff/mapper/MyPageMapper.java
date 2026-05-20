package com.commerce.bff.mapper;

import com.commerce.bff.dto.mypage.DeliveryDto;
import com.commerce.bff.dto.mypage.OrderDetailResponse;
import com.commerce.bff.dto.mypage.OrderItemDto;
import com.commerce.bff.dto.mypage.OrderListResponse;
import com.commerce.bff.dto.mypage.OrderSummaryDto;
import com.commerce.bff.dto.mypage.PointResponse;
import com.commerce.grpc.delivery.GetDeliveryStatusResponse;
import com.commerce.grpc.order.GetOrderListResponse;
import com.commerce.grpc.order.GetOrderStatusResponse;
import com.commerce.grpc.order.OrderItemProto;
import com.commerce.grpc.order.OrderSummary;
import com.commerce.grpc.point.GetPointBalanceResponse;
import org.mapstruct.Mapper;

/**
 * 마이페이지 gRPC 응답 → DTO 변환 매퍼
 *
 * - componentModel = "spring" : Spring @Component로 등록되어 @Autowired 가능
 * - 모든 메서드를 default로 구현 (프로토버프 빌더 패턴 호환)
 */
@Mapper(componentModel = "spring")
public interface MyPageMapper {

    // ── OrderSummary (주문 목록 내 단건) ─────────────────────────────────────────

    default OrderSummaryDto toOrderSummaryDto(OrderSummary o) {
        return OrderSummaryDto.builder()
                .orderId(o.getOrderId())
                .status(o.getStatus())
                .totalAmount(o.getTotalAmount())
                .createdAt(o.getCreatedAt())
                .build();
    }

    // ── OrderItem ────────────────────────────────────────────────────────────────

    default OrderItemDto toOrderItemDto(OrderItemProto item) {
        return OrderItemDto.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }

    // ── Delivery ─────────────────────────────────────────────────────────────────

    default DeliveryDto toDeliveryDto(GetDeliveryStatusResponse d) {
        return DeliveryDto.builder()
                .deliveryId(d.getDeliveryId())
                .status(d.getStatus())
                .address(d.getAddress())
                .startedAt(d.getStartedAt())
                .build();
    }

    // ── 주문 목록 응답 ────────────────────────────────────────────────────────────

    default OrderListResponse toOrderListResponse(String userId, GetOrderListResponse resp) {
        return OrderListResponse.builder()
                .userId(userId)
                .orders(resp.getOrdersList().stream()
                        .map(this::toOrderSummaryDto)
                        .toList())
                .currentPage(resp.getCurrentPage())
                .totalCount(resp.getTotalCount())
                .totalPages(resp.getTotalPages())
                .build();
    }

    // ── 주문 상세 응답 (주문 + 배송 통합) ─────────────────────────────────────────

    default OrderDetailResponse toOrderDetailResponse(GetOrderStatusResponse order,
                                                      GetDeliveryStatusResponse delivery) {
        return OrderDetailResponse.builder()
                .found(true)
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(order.getItemsList().stream()
                        .map(this::toOrderItemDto)
                        .toList())
                .delivery(delivery.getFound() ? toDeliveryDto(delivery) : null)
                .build();
    }

    // ── 주문 없음 응답 ─────────────────────────────────────────────────────────────

    default OrderDetailResponse toOrderDetailNotFound() {
        return OrderDetailResponse.builder()
                .found(false)
                .message("주문을 찾을 수 없습니다.")
                .build();
    }

    // ── 포인트 잔액 응답 ────────────────────────────────────────────────────────────

    default PointResponse toPointResponse(String userId, GetPointBalanceResponse resp) {
        return PointResponse.builder()
                .userId(userId)
                .totalPoint(resp.getTotalPoint())
                .found(resp.getFound())
                .build();
    }
}
