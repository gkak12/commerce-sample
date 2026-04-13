package com.commerce.order.kafka;

import com.commerce.common.kafka.KafkaTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Dead Letter Topic Consumer
 * 재시도 3회 소진 후 처리 실패한 메시지를 수신하여 알람/수동 재처리를 위해 로깅
 */
@Component
public class OrderDltConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderDltConsumer.class);

    @KafkaListener(
            topics = KafkaTopic.ORDER_CREATED_DLT,
            groupId = "order-service-dlt-group"
    )
    public void handleOrderCreatedDlt(ConsumerRecord<String, Object> record) {
        log.error("[DLT] Failed to process order.created after retries. " +
                        "key={}, offset={}, partition={}, payload={}",
                record.key(), record.offset(), record.partition(), record.value());
        // TODO: 슬랙/이메일 알람 또는 별도 실패 테이블에 저장
    }
}
