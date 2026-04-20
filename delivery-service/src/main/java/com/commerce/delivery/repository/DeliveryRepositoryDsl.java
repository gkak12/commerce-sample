package com.commerce.delivery.repository;

import com.commerce.delivery.entity.Delivery;
import com.commerce.delivery.entity.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryRepositoryDsl {

    /**
     * 사용자 ID + 배송 상태로 배송 내역 페이지 조회
     *
     * @param userId   사용자 ID (null 허용 → 조건 미적용)
     * @param status   배송 상태 (null 허용 → 조건 미적용)
     * @param pageable 페이지 정보
     * @return 페이지 결과
     */
    Page<Delivery> findByUserIdAndStatus(String userId, DeliveryStatus status, Pageable pageable);
}
