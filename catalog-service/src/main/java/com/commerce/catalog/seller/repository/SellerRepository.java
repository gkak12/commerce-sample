package com.commerce.catalog.seller.repository;

import com.commerce.catalog.seller.entity.Seller;
import com.commerce.catalog.seller.entity.SellerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findBySellerId(String sellerId);
    Optional<Seller> findByUserId(String userId);
    boolean existsByUserId(String userId);
    boolean existsByBusinessNumber(String businessNumber);
    Page<Seller> findAllByStatus(SellerStatus status, Pageable pageable);
}
