package com.commerce.catalog.product.repository;

import com.commerce.catalog.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByDepthOrderBySortOrder(int depth);
    List<Category> findAllByParentIdOrderBySortOrder(Long parentId);
}
