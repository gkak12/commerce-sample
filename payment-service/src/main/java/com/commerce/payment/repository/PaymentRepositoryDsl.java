package com.commerce.payment.repository;

import com.commerce.payment.entity.Payment;
import com.commerce.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentRepositoryDsl {

    /**
     * 사용자 ID + 결제 상태로 결제 내역 페이지 조회
     *
     * @param userId   사용자 ID (null 허용 → 조건 미적용)
     * @param status   결제 상태 (null 허용 → 조건 미적용)
     * @param pageable 페이지 정보
     * @return 페이지 결과
     */
    Page<Payment> findByUserIdAndStatus(String userId, PaymentStatus status, Pageable pageable);
}
