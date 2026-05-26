package com.commerce.bff.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP 요청 진입점에서 traceId를 추출/생성하여 MDC에 설정
 *
 * 흐름:
 *   HTTP 요청 → TraceIdFilter → MDC["traceId"] 설정
 *       → KafkaProducerTraceInterceptor가 MDC에서 traceId 읽어 Kafka 헤더에 삽입
 *       → 각 도메인 서비스 KafkaConsumer의 RecordInterceptor가 헤더에서 추출 → MDC 설정
 *       → 전체 흐름에서 동일 traceId로 로그 추적 가능
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 클라이언트가 traceId를 보내면 재사용, 없으면 신규 생성
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (StringUtils.isBlank(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        MDC.put(MDC_TRACE_KEY, traceId);
        // 응답 헤더에도 포함 — 클라이언트가 서버 로그와 매핑 가능
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_KEY);
        }
    }
}
