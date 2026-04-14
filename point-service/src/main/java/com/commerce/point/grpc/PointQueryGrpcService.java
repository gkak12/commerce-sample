package com.commerce.point.grpc;

import com.commerce.grpc.point.GetPointBalanceRequest;
import com.commerce.grpc.point.GetPointBalanceResponse;
import com.commerce.grpc.point.PointQueryServiceGrpc;
import com.commerce.point.repository.PointWalletRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포인트 잔액 조회 gRPC 서버
 *
 * - bff-service 가 마이페이지에서 포인트 잔액 조회 시 호출
 */
@GrpcService
@RequiredArgsConstructor
public class PointQueryGrpcService extends PointQueryServiceGrpc.PointQueryServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(PointQueryGrpcService.class);

    private final PointWalletRepository pointWalletRepository;

    @Override
    @Transactional(readOnly = true)
    public void getPointBalance(GetPointBalanceRequest request,
                                StreamObserver<GetPointBalanceResponse> responseObserver) {
        log.debug("[gRPC] getPointBalance. userId={}", request.getUserId());

        pointWalletRepository.findById(request.getUserId())
                .ifPresentOrElse(
                        wallet -> responseObserver.onNext(GetPointBalanceResponse.newBuilder()
                                .setFound(true)
                                .setUserId(wallet.getUserId())
                                .setTotalPoint(wallet.getTotalPoint())
                                .build()),
                        () -> responseObserver.onNext(GetPointBalanceResponse.newBuilder()
                                .setFound(false)
                                .setUserId(request.getUserId())
                                .setTotalPoint(0L)
                                .build())
                );

        responseObserver.onCompleted();
    }
}
