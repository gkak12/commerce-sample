package com.commerce.order.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    // FAILED 이벤트 중 최대 재시도 횟수 미만인 것만 조회 (재시도 대상)
    List<OutboxEvent> findTop100ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus status, int maxRetryCount);
}
