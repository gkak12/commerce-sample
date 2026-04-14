package com.commerce.bff.grpc;

import com.commerce.grpc.delivery.DeliveryQueryServiceGrpc;
import com.commerce.grpc.delivery.GetDeliveryStatusRequest;
import com.commerce.grpc.delivery.GetDeliveryStatusResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * delivery-service gRPC 클라이언트
 */
@Component
public class DeliveryGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(DeliveryGrpcClient.class);

    @GrpcClient("delivery-service")
    private DeliveryQueryServiceGrpc.DeliveryQueryServiceBlockingStub deliveryStub;

    public GetDeliveryStatusResponse getDeliveryStatus(String orderId, String userId) {
        log.debug("[gRPC-Client] getDeliveryStatus. orderId={}", orderId);
        return deliveryStub.getDeliveryStatus(GetDeliveryStatusRequest.newBuilder()
                .setOrderId(orderId)
                .setUserId(userId)
                .build());
    }
}
