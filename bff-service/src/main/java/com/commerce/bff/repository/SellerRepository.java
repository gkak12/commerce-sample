package com.commerce.bff.repository;

import com.commerce.bff.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findBySellerId(String sellerId);

    Optional<Seller> findByEmail(String email);

    boolean existsByEmail(String email);
}
