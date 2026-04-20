package com.commerce.order.repository;

import com.commerce.order.entity.Order;
import com.commerce.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryDsl {

    /**
     * 사용자 ID + 주문 상태로 주문 목록 페이지 조회
     *
     * @param userId   사용자 ID (null 허용 → 조건 미적용)
     * @param status   주문 상태 (null 허용 → 조건 미적용)
     * @param pageable 페이지 정보
     * @return 페이지 결과
     */
    Page<Order> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable);
}
