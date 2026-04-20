package com.commerce.order.grpc;

import com.commerce.grpc.order.GetOrderListRequest;
import com.commerce.grpc.order.GetOrderListResponse;
import com.commerce.grpc.order.GetOrderStatusRequest;
import com.commerce.grpc.order.GetOrderStatusResponse;
import com.commerce.grpc.order.OrderItemProto;
import com.commerce.grpc.order.OrderQueryServiceGrpc;
import com.commerce.grpc.order.OrderSummary;
import com.commerce.order.dto.OrderItemResponse;
import com.commerce.order.dto.OrderResponse;
import com.commerce.order.service.OrderService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * 주문 조회 gRPC 서버
 *
 * - bff-service 가 사용자의 주문 목록 / 단건 조회 시 호출
 * - HTTP REST 대신 gRPC 로 통신 → JSON 직렬화 비용 없음
 */
@GrpcService
@RequiredArgsConstructor
public class OrderQueryGrpcService extends OrderQueryServiceGrpc.OrderQueryServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(OrderQueryGrpcService.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderService orderService;

    // ── 단건 주문 상태 조회 ────────────────────────────────────────────────────

    @Override
    public void getOrderStatus(GetOrderStatusRequest request,
                               StreamObserver<GetOrderStatusResponse> responseObserver) {
        log.debug("[gRPC] getOrderStatus. orderId={}, userId={}", request.getOrderId(), request.getUserId());

        orderService.getOrder(request.getOrderId(), request.getUserId())
                .ifPresentOrElse(
                        order -> responseObserver.onNext(toStatusResponse(order)),
                        () -> responseObserver.onNext(GetOrderStatusResponse.newBuilder()
                                .setFound(false)
                                .build())
                );

        responseObserver.onCompleted();
    }

    // ── 주문 목록 페이지네이션 조회 ────────────────────────────────────────────

    @Override
    public void getOrderList(GetOrderListRequest request,
                             StreamObserver<GetOrderListResponse> responseObserver) {

        int page = Math.max(request.getPage(), 0);
        int size = (request.getSize() > 0)
                ? Math.min(request.getSize(), MAX_PAGE_SIZE)
                : DEFAULT_PAGE_SIZE;

        log.debug("[gRPC] getOrderList. userId={}, page={}, size={}", request.getUserId(), page, size);

        Page<OrderResponse> orderPage = orderService.getOrderList(
                request.getUserId(), PageRequest.of(page, size));

        List<OrderSummary> summaries = orderPage.getContent().stream()
                .map(this::toSummary)
                .toList();

        responseObserver.onNext(GetOrderListResponse.newBuilder()
                .addAllOrders(summaries)
                .setTotalCount((int) orderPage.getTotalElements())
                .setTotalPages(orderPage.getTotalPages())
                .setCurrentPage(orderPage.getNumber())
                .build());
        responseObserver.onCompleted();
    }

    // ── 변환 메서드 ───────────────────────────────────────────────────────────

    private GetOrderStatusResponse toStatusResponse(OrderResponse order) {
        List<OrderItemProto> itemProtos = order.items().stream()
                .map(this::toItemProto)
                .toList();

        return GetOrderStatusResponse.newBuilder()
                .setFound(true)
                .setOrderId(order.orderId())
                .setStatus(order.status())
                .setTotalAmount(order.totalAmount().toPlainString())
                .setCreatedAt(order.createdAt().toString())
                .addAllItems(itemProtos)
                .build();
    }

    private OrderSummary toSummary(OrderResponse order) {
        return OrderSummary.newBuilder()
                .setOrderId(order.orderId())
                .setStatus(order.status())
                .setTotalAmount(order.totalAmount().toPlainString())
                .setCreatedAt(order.createdAt().toString())
                .build();
    }

    private OrderItemProto toItemProto(OrderItemResponse item) {
        return OrderItemProto.newBuilder()
                .setProductId(item.productId())
                .setProductName(item.productName())
                .setQuantity(item.quantity())
                .setPrice(item.price().toPlainString())
                .build();
    }
}
