package com.commerce.point.monitoring;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

@Component
public class ConsumerLagMonitor {

    private static final Logger log = LoggerFactory.getLogger(ConsumerLagMonitor.class);
    private static final long LAG_WARN_THRESHOLD = 100L;
    private static final String GROUP_ID = "point-service-group";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Scheduled(fixedDelay = 30000)
    public void checkConsumerLag() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(props)) {
            ListConsumerGroupOffsetsResult offsetsResult =
                    adminClient.listConsumerGroupOffsets(GROUP_ID);

            Map<TopicPartition, OffsetAndMetadata> consumerOffsets =
                    offsetsResult.partitionsToOffsetAndMetadata().get();

            Map<TopicPartition, Long> endOffsets =
                    adminClient.listOffsets(
                            consumerOffsets.entrySet().stream()
                                    .collect(java.util.stream.Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> org.apache.kafka.clients.admin.OffsetSpec.latest()
                                    ))
                    ).all().get().entrySet().stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> e.getValue().offset()
                            ));

            long totalLag = 0;
            for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : consumerOffsets.entrySet()) {
                TopicPartition tp = entry.getKey();
                long consumerOffset = entry.getValue().offset();
                long endOffset = endOffsets.getOrDefault(tp, consumerOffset);
                long lag = Math.max(0, endOffset - consumerOffset);
                totalLag += lag;

                if (lag > LAG_WARN_THRESHOLD) {
                    log.warn("[ConsumerLag] HIGH LAG DETECTED! group={}, topic={}, partition={}, lag={}",
                            GROUP_ID, tp.topic(), tp.partition(), lag);
                } else {
                    log.debug("[ConsumerLag] group={}, topic={}, partition={}, lag={}",
                            GROUP_ID, tp.topic(), tp.partition(), lag);
                }
            }
            log.info("[ConsumerLag] Total lag for group={}: {}", GROUP_ID, totalLag);

        } catch (Exception e) {
            log.error("[ConsumerLag] Failed to check consumer lag for group={}", GROUP_ID, e);
        }
    }
}
