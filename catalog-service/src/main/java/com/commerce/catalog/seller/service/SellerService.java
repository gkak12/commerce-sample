package com.commerce.catalog.seller.service;

import com.commerce.catalog.seller.dto.SellerRegisterRequest;
import com.commerce.catalog.seller.dto.SellerResponse;
import com.commerce.catalog.seller.dto.SellerUpdateRequest;
import com.commerce.catalog.seller.entity.SellerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SellerService {

    /** 판매자 신청 (ROLE_USER → 승인 후 ROLE_SELLER 부여) */
    SellerResponse register(String userId, SellerRegisterRequest request);

    /** 내 판매자 정보 조회 */
    SellerResponse getMyInfo(String userId);

    /** 내 판매자 정보 수정 */
    SellerResponse updateMyInfo(String userId, SellerUpdateRequest request);

    // ── 관리자 전용 ───────────────────────────────────────────────────────────

    Page<SellerResponse> findAll(SellerStatus status, Pageable pageable);

    void approve(String sellerId);

    void reject(String sellerId);

    void suspend(String sellerId);
}
