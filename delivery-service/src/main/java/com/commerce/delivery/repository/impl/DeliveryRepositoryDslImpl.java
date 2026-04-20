package com.commerce.delivery.repository.impl;

import com.commerce.delivery.entity.Delivery;
import com.commerce.delivery.entity.DeliveryStatus;
import com.commerce.delivery.entity.QDelivery;
import com.commerce.delivery.repository.DeliveryRepositoryDsl;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class DeliveryRepositoryDslImpl implements DeliveryRepositoryDsl {

    private final JPAQueryFactory queryFactory;

    private static final QDelivery delivery = QDelivery.delivery;

    @Override
    public Page<Delivery> findByUserIdAndStatus(String userId, DeliveryStatus status, Pageable pageable) {
        List<Delivery> content = queryFactory
                .selectFrom(delivery)
                .where(
                        userIdEq(userId),
                        statusEq(status)
                )
                .orderBy(delivery.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(delivery.count())
                .from(delivery)
                .where(
                        userIdEq(userId),
                        statusEq(status)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    // null-safe BooleanExpression ─────────────────────────────

    private BooleanExpression userIdEq(String userId) {
        return userId != null ? delivery.userId.eq(userId) : null;
    }

    private BooleanExpression statusEq(DeliveryStatus status) {
        return status != null ? delivery.status.eq(status) : null;
    }
}
