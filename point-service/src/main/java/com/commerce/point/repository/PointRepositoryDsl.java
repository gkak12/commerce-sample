package com.commerce.point.repository;

import com.commerce.point.entity.Point;

import java.util.List;

public interface PointRepositoryDsl {
    List<Point> findByUserId(String userId);
}
