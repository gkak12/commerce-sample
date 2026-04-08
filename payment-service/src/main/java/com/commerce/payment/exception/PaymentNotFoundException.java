package com.commerce.payment.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String orderId) {
        super("결제 정보를 찾을 수 없습니다. orderId=" + orderId);
    }
}
