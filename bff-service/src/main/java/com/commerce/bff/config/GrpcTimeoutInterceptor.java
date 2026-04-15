package com.commerce.bff.config;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.TimeUnit;

/**
 * gRPC 전역 타임아웃 인터셉터
 *
 * - 모든 gRPC 클라이언트 호출에 deadline 자동 적용
 * - 이미 deadline이 설정된 호출은 그대로 유지 (덮어쓰지 않음)
 * - application.yml의 grpc.timeout-ms 값을 읽어 적용 (기본값 3초)
 *
 * @GrpcGlobalClientInterceptor → net.devh grpc-spring-boot-starter가 자동으로 모든 stub에 등록
 */
@GrpcGlobalClientInterceptor
public class GrpcTimeoutInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GrpcTimeoutInterceptor.class);

    @Value("${grpc.timeout-ms:3000}")
    private long timeoutMs;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        // 이미 deadline이 있는 경우 덮어쓰지 않음 (호출자가 명시적으로 설정한 경우 존중)
        if (callOptions.getDeadline() == null) {
            log.trace("[gRPC-Timeout] applying {}ms deadline. method={}", timeoutMs, method.getFullMethodName());
            callOptions = callOptions.withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS);
        }

        return next.newCall(method, callOptions);
    }
}
