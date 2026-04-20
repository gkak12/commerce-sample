package com.commerce.payment.repository.impl;

import com.commerce.payment.entity.Payment;
import com.commerce.payment.entity.PaymentStatus;
import com.commerce.payment.entity.QPayment;
import com.commerce.payment.repository.PaymentRepositoryDsl;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class PaymentRepositoryDslImpl implements PaymentRepositoryDsl {

    private final JPAQueryFactory queryFactory;

    private static final QPayment payment = QPayment.payment;

    @Override
    public Page<Payment> findByUserIdAndStatus(String userId, PaymentStatus status, Pageable pageable) {
        List<Payment> content = queryFactory
                .selectFrom(payment)
                .where(
                        userIdEq(userId),
                        statusEq(status)
                )
                .orderBy(payment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(payment.count())
                .from(payment)
                .where(
                        userIdEq(userId),
                        statusEq(status)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    // null-safe BooleanExpression ─────────────────────────────

    private BooleanExpression userIdEq(String userId) {
        return userId != null ? payment.userId.eq(userId) : null;
    }

    private BooleanExpression statusEq(PaymentStatus status) {
        return status != null ? payment.status.eq(status) : null;
    }
}
