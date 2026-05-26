package com.commerce.common.trace;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka Producer 인터셉터 — 분산 추적
 *
 * 모든 Kafka 메시지 발행 시 헤더에 traceId / spanId / sourceService 를 자동 삽입.
 * MDC에 traceId 가 존재하면 재사용, 없으면 신규 생성.
 *
 * <pre>
 * ProducerConfig.INTERCEPTOR_CLASSES_CONFIG = KafkaProducerTraceInterceptor.class.getName()
 * </pre>
 */
public class KafkaProducerTraceInterceptor implements ProducerInterceptor<String, Object> {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerTraceInterceptor.class);

    private String sourceService = "unknown-service";

    @Override
    public void configure(Map<String, ?> configs) {
        Object appName = configs.get("spring.application.name");
        if (appName != null) {
            this.sourceService = appName.toString();
        }
    }

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        // MDC에서 traceId 가져오기 (없으면 신규 생성)
        String traceId = MDC.get("traceId");
        if (StringUtils.isBlank(traceId)) {
            traceId = generateId();
        }
        String spanId = generateShortId();

        record.headers()
                .add(KafkaTraceHeaders.TRACE_ID, traceId.getBytes(StandardCharsets.UTF_8))
                .add(KafkaTraceHeaders.SPAN_ID, spanId.getBytes(StandardCharsets.UTF_8))
                .add(KafkaTraceHeaders.SOURCE_SERVICE, sourceService.getBytes(StandardCharsets.UTF_8))
                .add(KafkaTraceHeaders.EVENT_TYPE,
                        (record.value() != null ? record.value().getClass().getSimpleName() : "Unknown")
                                .getBytes(StandardCharsets.UTF_8));

        log.debug("[Trace][Producer] topic={}, traceId={}, spanId={}", record.topic(), traceId, spanId);
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            log.warn("[Trace][Producer] Send failed. topic={}, partition={}, error={}",
                    metadata != null ? metadata.topic() : "unknown",
                    metadata != null ? metadata.partition() : -1,
                    exception.getMessage());
        }
    }

    @Override
    public void close() {}

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateShortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
