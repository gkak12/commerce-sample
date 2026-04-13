package com.commerce.common.kafka;

public interface KafkaTopic {

    String ORDER_CREATED = "order.created";
    String ORDER_CONFIRMED = "order.confirmed";
    String PAYMENT_COMPLETED = "payment.completed";
    String DELIVERY_STARTED = "delivery.started";
    String POINT_EARNED = "point.earned";

    // Saga 보상 트랜잭션 토픽
    String PAYMENT_FAILED = "payment.failed";

    // Dead Letter Topics — 재시도 소진 후 실패한 메시지가 쌓이는 토픽
    String ORDER_CREATED_DLT = "order.created.DLT";
    String ORDER_CONFIRMED_DLT = "order.confirmed.DLT";
    String PAYMENT_COMPLETED_DLT = "payment.completed.DLT";
    String PAYMENT_FAILED_DLT = "payment.failed.DLT";
}
