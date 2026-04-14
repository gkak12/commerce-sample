package com.commerce.delivery.grpc;

import com.commerce.delivery.repository.DeliveryRepository;
import com.commerce.grpc.delivery.DeliveryQueryServiceGrpc;
import com.commerce.grpc.delivery.GetDeliveryStatusRequest;
import com.commerce.grpc.delivery.GetDeliveryStatusResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배송 상태 조회 gRPC 서버
 *
 * - bff-service 가 주문 상세 화면에서 배송 상태 조회 시 호출
 */
@GrpcService
@RequiredArgsConstructor
public class DeliveryQueryGrpcService extends DeliveryQueryServiceGrpc.DeliveryQueryServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(DeliveryQueryGrpcService.class);

    private final DeliveryRepository deliveryRepository;

    @Override
    @Transactional(readOnly = true)
    public void getDeliveryStatus(GetDeliveryStatusRequest request,
                                  StreamObserver<GetDeliveryStatusResponse> responseObserver) {
        log.debug("[gRPC] getDeliveryStatus. orderId={}, userId={}", request.getOrderId(), request.getUserId());

        // orderId로 배송 조회 후 userId 일치 검증
        deliveryRepository.findByOrderId(request.getOrderId())
                .filter(d -> d.getUserId().equals(request.getUserId()))
                .ifPresentOrElse(
                        delivery -> responseObserver.onNext(GetDeliveryStatusResponse.newBuilder()
                                .setFound(true)
                                .setDeliveryId(delivery.getDeliveryId())
                                .setOrderId(delivery.getOrderId())
                                .setStatus(delivery.getStatus().name())
                                .setAddress(delivery.getAddress())
                                .setStartedAt(delivery.getCreatedAt().toString())
                                .build()),
                        () -> responseObserver.onNext(GetDeliveryStatusResponse.newBuilder()
                                .setFound(false)
                                .build())
                );

        responseObserver.onCompleted();
    }
}
