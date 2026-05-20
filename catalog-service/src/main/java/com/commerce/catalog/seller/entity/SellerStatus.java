package com.commerce.catalog.seller.entity;

public enum SellerStatus {
    PENDING,    // 승인 대기
    APPROVED,   // 승인 완료
    REJECTED,   // 거절
    SUSPENDED,  // 정지
    WITHDRAWN   // 탈퇴
}
