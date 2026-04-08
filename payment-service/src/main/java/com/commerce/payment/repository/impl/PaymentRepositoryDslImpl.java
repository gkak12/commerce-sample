package com.commerce.payment.repository.impl;

import com.commerce.payment.entity.Payment;
import com.commerce.payment.entity.QPayment;
import com.commerce.payment.repository.PaymentRepositoryDsl;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryDslImpl implements PaymentRepositoryDsl {

    private final JPAQueryFactory queryFactory;

    private final QPayment payment = QPayment.payment;

    @Override
    public List<Payment> findByUserId(String userId) {
        return queryFactory
                .selectFrom(payment)
                .where(payment.userId.eq(userId))
                .orderBy(payment.createdAt.desc())
                .fetch();
    }
}
