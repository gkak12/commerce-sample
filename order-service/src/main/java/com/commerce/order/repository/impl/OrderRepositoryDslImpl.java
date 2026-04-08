package com.commerce.order.repository.impl;

import com.commerce.order.entity.Order;
import com.commerce.order.entity.OrderStatus;
import com.commerce.order.entity.QOrder;
import com.commerce.order.repository.OrderRepositoryDsl;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryDslImpl implements OrderRepositoryDsl {

    private final JPAQueryFactory queryFactory;

    private final QOrder order = QOrder.order;

    @Override
    public List<Order> findByUserIdAndStatus(String userId, OrderStatus status) {
        return queryFactory
                .selectFrom(order)
                .where(
                        order.userId.eq(userId),
                        order.status.eq(status)
                )
                .orderBy(order.createdAt.desc())
                .fetch();
    }
}
