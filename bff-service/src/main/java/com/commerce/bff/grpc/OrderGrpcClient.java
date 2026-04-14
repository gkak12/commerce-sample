package com.commerce.bff.grpc;

import com.commerce.grpc.order.GetOrderListRequest;
import com.commerce.grpc.order.GetOrderListResponse;
import com.commerce.grpc.order.GetOrderStatusRequest;
import com.commerce.grpc.order.GetOrderStatusResponse;
import com.commerce.grpc.order.OrderQueryServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * order-service gRPC 클라이언트
 *
 * @GrpcClient("order-service") → application.yml의 grpc.client.order-service 설정 사용
 */
@Component
public class OrderGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(OrderGrpcClient.class);

    // BlockingStub: 동기 호출 (REST처럼 응답을 기다림)
    @GrpcClient("order-service")
    private OrderQueryServiceGrpc.OrderQueryServiceBlockingStub orderStub;

    public GetOrderStatusResponse getOrderStatus(String orderId, String userId) {
        log.debug("[gRPC-Client] getOrderStatus. orderId={}", orderId);
        return orderStub.getOrderStatus(GetOrderStatusRequest.newBuilder()
                .setOrderId(orderId)
                .setUserId(userId)
                .build());
    }

    public GetOrderListResponse getOrderList(String userId) {
        log.debug("[gRPC-Client] getOrderList. userId={}", userId);
        return orderStub.getOrderList(GetOrderListRequest.newBuilder()
                .setUserId(userId)
                .build());
    }
}
