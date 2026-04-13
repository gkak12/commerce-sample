package com.commerce.point.repository;

import com.commerce.point.entity.Point;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointRepository extends JpaRepository<Point, Long>, PointRepositoryDsl {

    boolean existsByOrderId(String orderId);
}
