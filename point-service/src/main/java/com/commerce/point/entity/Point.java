package com.commerce.point.entity;

import com.commerce.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "points")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Point extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 36)
    private String orderId;

    @Column(nullable = false)
    private Long earnedPoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointType type;
}
