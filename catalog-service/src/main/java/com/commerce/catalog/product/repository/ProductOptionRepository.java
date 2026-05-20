package com.commerce.catalog.product.repository;

import com.commerce.catalog.product.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

    @Query("SELECT po FROM ProductOption po JOIN po.product p WHERE p.productId = :productId")
    List<ProductOption> findByProductId(@Param("productId") String productId);

    /**
     * 재고 차감 — stock_quantity가 quantity 이상인 옵션만 차감 (음수 방지)
     */
    @Modifying
    @Query("""
           UPDATE ProductOption po
           SET po.stockQuantity = po.stockQuantity - :quantity
           WHERE po.id IN (
               SELECT po2.id FROM ProductOption po2
               JOIN po2.product p WHERE p.productId = :productId
           )
           AND po.stockQuantity >= :quantity
           """)
    int decreaseStock(@Param("productId") String productId, @Param("quantity") int quantity);

    /**
     * 재고 복구
     */
    @Modifying
    @Query("""
           UPDATE ProductOption po
           SET po.stockQuantity = po.stockQuantity + :quantity
           WHERE po.id IN (
               SELECT po2.id FROM ProductOption po2
               JOIN po2.product p WHERE p.productId = :productId
           )
           """)
    int restoreStock(@Param("productId") String productId, @Param("quantity") int quantity);
}
