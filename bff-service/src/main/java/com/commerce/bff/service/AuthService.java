package com.commerce.bff.service;

import com.commerce.bff.dto.auth.*;

/**
 * 일반 회원 인증 서비스
 */
public interface AuthService {

    /** 일반 회원가입 — 가입 즉시 토큰 발급 */
    AuthTokens signup(SignupRequest request, String ip);

    AuthTokens login(LoginRequest request, String ip);

    AuthTokens refresh(String refreshToken);

    void logout(String userId, String accessToken);

    void changePassword(String userId, String accessToken, ChangePasswordRequest request);
}
