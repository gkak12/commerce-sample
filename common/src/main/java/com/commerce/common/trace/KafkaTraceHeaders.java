package com.commerce.common.trace;

/**
 * Kafka 메시지 헤더 키 상수
 * Producer → Consumer 간 분산 추적 정보를 전달하는 데 사용
 */
public final class KafkaTraceHeaders {

    private KafkaTraceHeaders() {}

    /** 전체 요청 흐름을 추적하는 ID (HTTP 요청부터 최종 이벤트까지 동일) */
    public static final String TRACE_ID = "X-Trace-Id";

    /** 개별 Kafka 메시지 단위의 ID */
    public static final String SPAN_ID = "X-Span-Id";

    /** 이벤트를 발행한 서비스 이름 */
    public static final String SOURCE_SERVICE = "X-Source-Service";

    /** 이벤트 타입 (예: OrderCreatedEvent) */
    public static final String EVENT_TYPE = "X-Event-Type";
}
