package com.commerce.point.entity;

import com.commerce.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_wallets")
@Getter
@NoArgsConstructor
public class PointWallet extends BaseEntity {

    @Id
    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false)
    private long totalPoint;

    public PointWallet(String userId) {
        this.userId = userId;
        this.totalPoint = 0L;   // 초기 잔액 고정
    }

    // ── 상태 변경 비즈니스 메서드 ─────────────────────────────────────────────

    public void earn(long point) {
        this.totalPoint += point;
    }
}
