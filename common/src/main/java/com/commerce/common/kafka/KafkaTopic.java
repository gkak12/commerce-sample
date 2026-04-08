package com.commerce.common.kafka;

public interface KafkaTopic {

    String ORDER_CREATED = "order.created";
    String ORDER_CONFIRMED = "order.confirmed";
    String PAYMENT_COMPLETED = "payment.completed";
    String DELIVERY_STARTED = "delivery.started";
    String POINT_EARNED = "point.earned";
}
