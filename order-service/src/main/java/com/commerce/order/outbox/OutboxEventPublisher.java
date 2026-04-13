package com.commerce.order.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public OutboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents =
                outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) return;

        log.debug("[Outbox] Processing {} pending events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() {});

                // EOS: Kafka 트랜잭션으로 정확히 한 번 발행 보장
                kafkaTemplate.executeInTransaction(ops -> {
                    ops.send(event.getTopic(), event.getAggregateId(), payload);
                    return null;
                });

                // Kafka 발행 성공 후 별도 DB 트랜잭션으로 상태 업데이트
                transactionTemplate.execute(status -> {
                    outboxEventRepository.findById(event.getId()).ifPresent(e -> {
                        e.markPublished();
                        outboxEventRepository.save(e);
                    });
                    return null;
                });

                log.info("[Outbox] Published. topic={}, aggregateId={}, id={}",
                        event.getTopic(), event.getAggregateId(), event.getId());

            } catch (Exception e) {
                log.error("[Outbox] Failed to publish. id={}, topic={}, retry={}",
                        event.getId(), event.getTopic(), event.getRetryCount(), e);

                transactionTemplate.execute(status -> {
                    outboxEventRepository.findById(event.getId()).ifPresent(ev -> {
                        ev.markFailed();
                        outboxEventRepository.save(ev);
                    });
                    return null;
                });
            }
        }
    }
}
