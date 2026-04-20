package com.commerce.order.repository.impl;

import com.commerce.order.entity.Order;
import com.commerce.order.entity.OrderStatus;
import com.commerce.order.entity.QOrder;
import com.commerce.order.repository.OrderRepositoryDsl;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class OrderRepositoryDslImpl implements OrderRepositoryDsl {

    private final JPAQueryFactory queryFactory;

    private static final QOrder order = QOrder.order;

    @Override
    public Page<Order> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable) {
        List<Order> content = queryFactory
                .selectFrom(order)
                .where(
                        userIdEq(userId),
                        statusEq(status)
                )
                .orderBy(order.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(order.count())
                .from(order)
                .where(
                        userIdEq(userId),
                        statusEq(status)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    // null-safe BooleanExpression ─────────────────────────────

    private BooleanExpression userIdEq(String userId) {
        return userId != null ? order.userId.eq(userId) : null;
    }

    private BooleanExpression statusEq(OrderStatus status) {
        return status != null ? order.status.eq(status) : null;
    }
}
