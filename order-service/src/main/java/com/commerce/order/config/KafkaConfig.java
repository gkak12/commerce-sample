package com.commerce.order.config;

import com.commerce.common.trace.KafkaProducerTraceInterceptor;
import com.commerce.common.trace.KafkaTraceHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.application.name}")
    private String applicationName;

    private ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ── Producer (EOS) ───────────────────────────────────────────────────────
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // EOS: 멱등 프로듀서 — 브로커가 중복 메시지를 자동 제거
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // EOS: 트랜잭션 ID — 서비스 인스턴스마다 고유해야 함
        config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, applicationName + "-tx-" + UUID.randomUUID());

        // 분산 추적 인터셉터
        config.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, KafkaProducerTraceInterceptor.class.getName());
        config.put("spring.application.name", applicationName);

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(config);
        factory.setValueSerializer(new JsonSerializer<>(objectMapper()));
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ── Consumer (read_committed) ─────────────────────────────────────────────
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        JsonDeserializer<Object> deserializer = new JsonDeserializer<>(objectMapper());
        deserializer.addTrustedPackages("com.commerce.common.event");

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // EOS: 트랜잭션 커밋된 메시지만 읽음 (uncommitted 메시지 무시)
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    // ── 분산 추적 RecordInterceptor ───────────────────────────────────────────
    @Bean
    public RecordInterceptor<String, Object> traceRecordInterceptor() {
        return new RecordInterceptor<>() {
            @Override
            public ConsumerRecord<String, Object> intercept(
                    ConsumerRecord<String, Object> record,
                    org.apache.kafka.clients.consumer.Consumer<String, Object> consumer) {

                // Kafka 헤더에서 traceId 추출 → MDC 설정
                Header traceHeader = record.headers().lastHeader(KafkaTraceHeaders.TRACE_ID);
                String traceId = (traceHeader != null)
                        ? new String(traceHeader.value(), StandardCharsets.UTF_8)
                        : UUID.randomUUID().toString().replace("-", "");

                Header sourceHeader = record.headers().lastHeader(KafkaTraceHeaders.SOURCE_SERVICE);
                String sourceService = (sourceHeader != null)
                        ? new String(sourceHeader.value(), StandardCharsets.UTF_8)
                        : "unknown";

                MDC.put("traceId", traceId);
                MDC.put("sourceService", sourceService);
                MDC.put("kafkaTopic", record.topic());
                MDC.put("kafkaOffset", String.valueOf(record.offset()));
                return record;
            }

            @Override
            public void afterRecord(
                    ConsumerRecord<String, Object> record,
                    org.apache.kafka.clients.consumer.Consumer<String, Object> consumer) {
                MDC.remove("traceId");
                MDC.remove("sourceService");
                MDC.remove("kafkaTopic");
                MDC.remove("kafkaOffset");
            }
        };
    }

    // ── 토픽 자동 생성 (3 파티션) ─────────────────────────────────────────────
    @Bean
    public org.apache.kafka.clients.admin.NewTopic orderCreatedTopic() {
        return TopicBuilder.name(com.commerce.common.kafka.KafkaTopic.ORDER_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic orderConfirmedTopic() {
        return TopicBuilder.name(com.commerce.common.kafka.KafkaTopic.ORDER_CONFIRMED).partitions(3).replicas(1).build();
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic paymentFailedTopic() {
        return TopicBuilder.name(com.commerce.common.kafka.KafkaTopic.PAYMENT_FAILED).partitions(3).replicas(1).build();
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic stockRestoreTopic() {
        return TopicBuilder.name(com.commerce.common.kafka.KafkaTopic.STOCK_RESTORE).partitions(3).replicas(1).build();
    }

    // ── 에러 핸들러 (지수 백오프 DLT) ─────────────────────────────────────────
    @Bean
    public CommonErrorHandler errorHandler() {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate());
        // 지수 백오프: 2s → 4s → 8s, 최대 3회
        ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
        backOff.setMaxAttempts(3);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler());
        factory.setRecordInterceptor(traceRecordInterceptor());
        factory.setConcurrency(3);
        return factory;
    }
}
