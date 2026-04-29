package com.commerce.bff.service;

import com.commerce.bff.dto.auth.LoginRequest;
import com.commerce.bff.dto.auth.RefreshTokenRequest;
import com.commerce.bff.dto.auth.SignupRequest;
import com.commerce.bff.dto.auth.TokenResponse;

public interface AuthService {

    TokenResponse signup(SignupRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    void logout(String userId);
}
