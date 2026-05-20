package com.commerce.common.kafka;

public interface KafkaTopic {

    String ORDER_CREATED = "order.created";
    String ORDER_CONFIRMED = "order.confirmed";
    String PAYMENT_COMPLETED = "payment.completed";
    String DELIVERY_STARTED = "delivery.started";
    String POINT_EARNED = "point.earned";

    // 주문 완료 / 취소 (order-service 발행 → 알림 등 구독)
    String ORDER_COMPLETED = "order.completed";
    String ORDER_CANCELLED = "order.cancelled";

    // 사용자 주문 취소 요청 (bff-service 발행 → order-service 처리)
    String ORDER_CANCEL_REQUESTED = "order.cancel.requested";

    // Saga 보상 트랜잭션 토픽
    String PAYMENT_FAILED = "payment.failed";

    // Redis 재고 복구 토픽 — 주문 취소/결제 실패 시 bff-service가 소비하여 재고 복원
    String STOCK_RESTORE = "stock.restore";

    // 판매자 승인 — catalog-service 발행 → bff-service 소비 (role USER → SELLER 변경)
    String SELLER_APPROVED = "seller.approved";

    // Dead Letter Topics — 재시도 소진 후 실패한 메시지가 쌓이는 토픽
    String ORDER_CREATED_DLT = "order.created.DLT";
    String ORDER_CONFIRMED_DLT = "order.confirmed.DLT";
    String PAYMENT_COMPLETED_DLT = "payment.completed.DLT";
    String PAYMENT_FAILED_DLT = "payment.failed.DLT";
}
