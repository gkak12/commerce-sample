package com.commerce.bff.repository;

import com.commerce.bff.entity.AuthProvider;
import com.commerce.bff.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    Optional<User> findByEmailAndProvider(String email, AuthProvider provider);

    Optional<User> findByEmail(String email);
}
