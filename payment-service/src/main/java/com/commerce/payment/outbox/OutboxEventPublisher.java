package com.commerce.payment.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
    private static final int MAX_RETRY_COUNT = 5;

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
    @SchedulerLock(name = "outbox_publisher_payment", lockAtMostFor = "PT10S", lockAtLeastFor = "PT4S")
    public void publishPendingEvents() {
        processEvents(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING));
    }

    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(name = "outbox_retry_payment", lockAtMostFor = "PT30S", lockAtLeastFor = "PT10S")
    public void retryFailedEvents() {
        List<OutboxEvent> events =
                outboxEventRepository.findTop100ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                        OutboxStatus.FAILED, MAX_RETRY_COUNT);
        if (!events.isEmpty()) {
            log.info("[Outbox][Retry] FAILED 이벤트 재시도. count={}", events.size());
        }
        processEvents(events);
    }

    private void processEvents(List<OutboxEvent> events) {
        if (events.isEmpty()) return;

        log.debug("[Outbox] Processing {} events", events.size());

        for (OutboxEvent event : events) {
            try {
                Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() {});

                kafkaTemplate.executeInTransaction(ops -> {
                    ops.send(event.getTopic(), event.getAggregateId(), payload);
                    return null;
                });

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
                log.error("[Outbox] Failed to publish. id={}, topic={}, retryCount={}",
                        event.getId(), event.getTopic(), event.getRetryCount(), e);

                transactionTemplate.execute(status -> {
                    outboxEventRepository.findById(event.getId()).ifPresent(ev -> {
                        ev.markFailed();
                        if (ev.getRetryCount() >= MAX_RETRY_COUNT) {
                            log.error("[Outbox] 최대 재시도 횟수 초과. 수동 개입 필요. id={}, topic={}",
                                    ev.getId(), ev.getTopic());
                        }
                        outboxEventRepository.save(ev);
                    });
                    return null;
                });
            }
        }
    }
}
