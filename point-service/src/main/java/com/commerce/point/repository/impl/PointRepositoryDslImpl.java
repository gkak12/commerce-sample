package com.commerce.point.repository.impl;

import com.commerce.point.entity.Point;
import com.commerce.point.entity.PointType;
import com.commerce.point.entity.QPoint;
import com.commerce.point.repository.PointRepositoryDsl;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class PointRepositoryDslImpl implements PointRepositoryDsl {

    private final JPAQueryFactory queryFactory;

    private static final QPoint point = QPoint.point;

    @Override
    public Page<Point> findByUserIdAndType(String userId, PointType type, Pageable pageable) {
        List<Point> content = queryFactory
                .selectFrom(point)
                .where(
                        userIdEq(userId),
                        typeEq(type)
                )
                .orderBy(point.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(point.count())
                .from(point)
                .where(
                        userIdEq(userId),
                        typeEq(type)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    // null-safe BooleanExpression ─────────────────────────────

    private BooleanExpression userIdEq(String userId) {
        return userId != null ? point.userId.eq(userId) : null;
    }

    private BooleanExpression typeEq(PointType type) {
        return type != null ? point.type.eq(type) : null;
    }
}
