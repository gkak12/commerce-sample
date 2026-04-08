package com.commerce.point.entity;

import com.commerce.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "point_wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointWallet extends BaseEntity {

    @Id
    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false)
    private long totalPoint;

    public void earn(long point) {
        this.totalPoint += point;
    }
}
