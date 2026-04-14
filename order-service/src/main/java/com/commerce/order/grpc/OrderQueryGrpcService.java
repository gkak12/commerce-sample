package com.commerce.order.grpc;

import com.commerce.grpc.order.GetOrderListRequest;
import com.commerce.grpc.order.GetOrderListResponse;
import com.commerce.grpc.order.GetOrderStatusRequest;
import com.commerce.grpc.order.GetOrderStatusResponse;
import com.commerce.grpc.order.OrderItemProto;
import com.commerce.grpc.order.OrderQueryServiceGrpc;
import com.commerce.grpc.order.OrderSummary;
import com.commerce.order.entity.Order;
import com.commerce.order.entity.OrderItem;
import com.commerce.order.repository.OrderRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

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

    private final OrderRepository orderRepository;

    // ── 단건 주문 상태 조회 ────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public void getOrderStatus(GetOrderStatusRequest request,
                               StreamObserver<GetOrderStatusResponse> responseObserver) {
        log.debug("[gRPC] getOrderStatus. orderId={}, userId={}", request.getOrderId(), request.getUserId());

        orderRepository.findById(request.getOrderId())
                .filter(o -> o.getUserId().equals(request.getUserId()))  // 본인 주문 검증
                .ifPresentOrElse(
                        order -> {
                            List<OrderItemProto> itemProtos = order.getOrderItems().stream()
                                    .map(this::toItemProto)
                                    .toList();

                            responseObserver.onNext(GetOrderStatusResponse.newBuilder()
                                    .setFound(true)
                                    .setOrderId(order.getOrderId())
                                    .setStatus(order.getStatus().name())
                                    .setTotalAmount(order.getTotalAmount().toPlainString())
                                    .setCreatedAt(order.getCreatedAt().toString())
                                    .addAllItems(itemProtos)
                                    .build());
                        },
                        () -> responseObserver.onNext(GetOrderStatusResponse.newBuilder()
                                .setFound(false)
                                .build())
                );

        responseObserver.onCompleted();
    }

    // ── 사용자 전체 주문 목록 조회 ─────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public void getOrderList(GetOrderListRequest request,
                             StreamObserver<GetOrderListResponse> responseObserver) {
        log.debug("[gRPC] getOrderList. userId={}", request.getUserId());

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(request.getUserId());

        List<OrderSummary> summaries = orders.stream()
                .map(o -> OrderSummary.newBuilder()
                        .setOrderId(o.getOrderId())
                        .setStatus(o.getStatus().name())
                        .setTotalAmount(o.getTotalAmount().toPlainString())
                        .setCreatedAt(o.getCreatedAt().toString())
                        .build())
                .toList();

        responseObserver.onNext(GetOrderListResponse.newBuilder()
                .addAllOrders(summaries)
                .build());
        responseObserver.onCompleted();
    }

    private OrderItemProto toItemProto(OrderItem item) {
        return OrderItemProto.newBuilder()
                .setProductId(item.getProductId())
                .setProductName(item.getProductName())
                .setQuantity(item.getQuantity())
                .setPrice(item.getPrice().toPlainString())
                .build();
    }
}
