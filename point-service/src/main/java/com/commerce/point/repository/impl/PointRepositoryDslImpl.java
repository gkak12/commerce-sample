package com.commerce.point.repository.impl;

import com.commerce.point.entity.Point;
import com.commerce.point.entity.QPoint;
import com.commerce.point.repository.PointRepositoryDsl;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PointRepositoryDslImpl implements PointRepositoryDsl {

    private final JPAQueryFactory queryFactory;

    private final QPoint point = QPoint.point;

    @Override
    public List<Point> findByUserId(String userId) {
        return queryFactory
                .selectFrom(point)
                .where(point.userId.eq(userId))
                .orderBy(point.createdAt.desc())
                .fetch();
    }
}
