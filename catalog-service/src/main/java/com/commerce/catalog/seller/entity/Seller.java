package com.commerce.catalog.seller.entity;

import com.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sellers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Seller extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sellerId;          // UUID (외부 노출용)

    @Column(nullable = false, unique = true)
    private String userId;            // bff-service users.userId (크로스 서비스 참조)

    @Column(nullable = false)
    private String businessName;      // 상호명

    @Column(nullable = false, unique = true)
    private String businessNumber;    // 사업자등록번호

    @Column(nullable = false)
    private String ownerName;         // 대표자명

    @Column(nullable = false)
    private String phone;             // 연락처

    @Column(nullable = false)
    private String email;             // 정산/공지 수신 이메일

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SellerStatus status;

    // ── 도메인 메서드 ────────────────────────────────────────────────────────

    public void approve() {
        this.status = SellerStatus.APPROVED;
    }

    public void reject() {
        this.status = SellerStatus.REJECTED;
    }

    public void suspend() {
        this.status = SellerStatus.SUSPENDED;
    }

    public void withdraw() {
        this.status = SellerStatus.WITHDRAWN;
    }

    public void updateInfo(String businessName, String phone, String email) {
        this.businessName = businessName;
        this.phone        = phone;
        this.email        = email;
    }

    public boolean isApproved() {
        return this.status == SellerStatus.APPROVED;
    }
}
