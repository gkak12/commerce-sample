package com.commerce.bff.service;

import com.commerce.bff.dto.auth.*;

/**
 * 판매자 인증 서비스
 * - 이메일/비밀번호 전용 (소셜 로그인 미지원)
 */
public interface SellerAuthService {

    /** 판매자 계정 생성 — 가입 즉시 토큰 발급 (사업자 정보 제출은 별도) */
    AuthTokens signup(SellerSignupRequest request, String ip);

    AuthTokens login(LoginRequest request, String ip);

    AuthTokens refresh(String refreshToken);

    void logout(String sellerId, String accessToken);

    void changePassword(String sellerId, String accessToken, ChangePasswordRequest request);
}
