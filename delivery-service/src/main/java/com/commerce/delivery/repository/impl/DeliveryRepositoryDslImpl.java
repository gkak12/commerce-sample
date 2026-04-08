package com.commerce.delivery.repository.impl;

import com.commerce.delivery.entity.Delivery;
import com.commerce.delivery.entity.QDelivery;
import com.commerce.delivery.repository.DeliveryRepositoryDsl;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DeliveryRepositoryDslImpl implements DeliveryRepositoryDsl {

    private final JPAQueryFactory queryFactory;

    private final QDelivery delivery = QDelivery.delivery;

    @Override
    public List<Delivery> findByUserId(String userId) {
        return queryFactory
                .selectFrom(delivery)
                .where(delivery.userId.eq(userId))
                .orderBy(delivery.createdAt.desc())
                .fetch();
    }
}
