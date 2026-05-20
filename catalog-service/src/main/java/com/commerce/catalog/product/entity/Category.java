package com.commerce.catalog.product.entity;

import com.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** 상위 카테고리 ID (null이면 최상위) */
    private Long parentId;

    /** 1: 대분류, 2: 중분류, 3: 소분류 */
    @Column(nullable = false)
    private int depth;

    @Column(nullable = false)
    private int sortOrder;
}
