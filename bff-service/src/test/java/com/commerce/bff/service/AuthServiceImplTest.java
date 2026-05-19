package com.commerce.bff.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl 단위 테스트")
public class AuthServiceImplTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("Bcrpyt 암호화 테스트")
    void encryptPassword() {
        String password = "test1234";
        String encPassword = passwordEncoder.encode(password);
        System.out.println(String.format("encPassword: %s", encPassword));
    }
}
