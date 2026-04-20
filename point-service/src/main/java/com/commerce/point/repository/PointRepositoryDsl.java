package com.commerce.point.repository;

import com.commerce.point.entity.Point;
import com.commerce.point.entity.PointType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointRepositoryDsl {

    /**
     * 사용자 ID + 포인트 유형으로 포인트 내역 페이지 조회
     *
     * @param userId   사용자 ID (null 허용 → 조건 미적용)
     * @param type     포인트 유형 (null 허용 → 조건 미적용)
     * @param pageable 페이지 정보
     * @return 페이지 결과
     */
    Page<Point> findByUserIdAndType(String userId, PointType type, Pageable pageable);
}
