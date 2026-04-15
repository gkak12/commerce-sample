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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;

    // ── 단건 주문 상태 조회 ────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public void getOrderStatus(GetOrderStatusRequest request,
                               StreamObserver<GetOrderStatusResponse> responseObserver) {
        log.debug("[gRPC] getOrderStatus. orderId={}, userId={}", request.getOrderId(), request.getUserId());

        orderRepository.findById(request.getOrderId())
                .filter(o -> o.getUserId().equals(request.getUserId()))
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

    // ── 주문 목록 페이지네이션 조회 ────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public void getOrderList(GetOrderListRequest request,
                             StreamObserver<GetOrderListResponse> responseObserver) {

        int page = Math.max(request.getPage(), 0);
        int size = (request.getSize() > 0)
                ? Math.min(request.getSize(), MAX_PAGE_SIZE)
                : DEFAULT_PAGE_SIZE;

        log.debug("[gRPC] getOrderList. userId={}, page={}, size={}", request.getUserId(), page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(request.getUserId(), pageable);

        List<OrderSummary> summaries = orderPage.getContent().stream()
                .map(o -> OrderSummary.newBuilder()
                        .setOrderId(o.getOrderId())
                        .setStatus(o.getStatus().name())
                        .setTotalAmount(o.getTotalAmount().toPlainString())
                        .setCreatedAt(o.getCreatedAt().toString())
                        .build())
                .toList();

        responseObserver.onNext(GetOrderListResponse.newBuilder()
                .addAllOrders(summaries)
                .setTotalCount((int) orderPage.getTotalElements())
                .setTotalPages(orderPage.getTotalPages())
                .setCurrentPage(orderPage.getNumber())
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
