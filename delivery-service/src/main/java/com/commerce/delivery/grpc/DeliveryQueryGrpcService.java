package com.commerce.delivery.grpc;

import com.commerce.delivery.dto.DeliveryResponse;
import com.commerce.delivery.service.DeliveryService;
import com.commerce.grpc.delivery.DeliveryQueryServiceGrpc;
import com.commerce.grpc.delivery.GetDeliveryStatusRequest;
import com.commerce.grpc.delivery.GetDeliveryStatusResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 배송 상태 조회 gRPC 서버
 *
 * - bff-service 가 주문 상세 화면에서 배송 상태 조회 시 호출
 */
@GrpcService
@RequiredArgsConstructor
public class DeliveryQueryGrpcService extends DeliveryQueryServiceGrpc.DeliveryQueryServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(DeliveryQueryGrpcService.class);

    private final DeliveryService deliveryService;

    @Override
    public void getDeliveryStatus(GetDeliveryStatusRequest request,
                                  StreamObserver<GetDeliveryStatusResponse> responseObserver) {
        log.debug("[gRPC] getDeliveryStatus. orderId={}, userId={}", request.getOrderId(), request.getUserId());

        deliveryService.getDelivery(request.getOrderId(), request.getUserId())
                .ifPresentOrElse(
                        delivery -> responseObserver.onNext(toResponse(delivery)),
                        () -> responseObserver.onNext(GetDeliveryStatusResponse.newBuilder()
                                .setFound(false)
                                .build())
                );

        responseObserver.onCompleted();
    }

    private GetDeliveryStatusResponse toResponse(DeliveryResponse delivery) {
        return GetDeliveryStatusResponse.newBuilder()
                .setFound(true)
                .setDeliveryId(delivery.deliveryId())
                .setOrderId(delivery.orderId())
                .setStatus(delivery.status())
                .setAddress(delivery.address())
                .setStartedAt(delivery.createdAt().toString())
                .build();
    }
}
