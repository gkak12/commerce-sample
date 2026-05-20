package com.commerce.bff.repository;

import com.commerce.bff.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByAdminId(String adminId);

    Optional<Admin> findByEmail(String email);

    boolean existsByEmail(String email);
}
