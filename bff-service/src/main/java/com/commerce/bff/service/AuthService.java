package com.commerce.bff.service;

import com.commerce.bff.dto.auth.AuthTokens;
import com.commerce.bff.dto.auth.LoginRequest;
import com.commerce.bff.dto.auth.SignupRequest;

public interface AuthService {

    AuthTokens signup(SignupRequest request);

    AuthTokens login(LoginRequest request);

    /** @param refreshToken HttpOnly Cookie에서 추출한 Refresh Token */
    AuthTokens refresh(String refreshToken);

    void logout(String userId);
}
