package com.commerce.bff.grpc;

import com.commerce.grpc.point.GetPointBalanceRequest;
import com.commerce.grpc.point.GetPointBalanceResponse;
import com.commerce.grpc.point.PointQueryServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * point-service gRPC 클라이언트
 */
@Component
public class PointGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(PointGrpcClient.class);

    @GrpcClient("point-service")
    private PointQueryServiceGrpc.PointQueryServiceBlockingStub pointStub;

    public GetPointBalanceResponse getPointBalance(String userId) {
        log.debug("[gRPC-Client] getPointBalance. userId={}", userId);
        return pointStub.getPointBalance(GetPointBalanceRequest.newBuilder()
                .setUserId(userId)
                .build());
    }
}
