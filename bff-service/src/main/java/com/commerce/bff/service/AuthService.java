package com.commerce.bff.service;

import com.commerce.bff.dto.auth.AuthTokens;
import com.commerce.bff.dto.auth.ChangePasswordRequest;
import com.commerce.bff.dto.auth.LoginRequest;
import com.commerce.bff.dto.auth.SignupRequest;

public interface AuthService {

    AuthTokens signup(SignupRequest request);

    AuthTokens login(LoginRequest request);

    /** @param refreshToken HttpOnly Cookie에서 추출한 Refresh Token
     *  @param deviceId     X-Device-Id 헤더에서 추출한 기기 식별자 */
    AuthTokens refresh(String refreshToken, String deviceId);

    /** @param accessToken 블랙리스트 등록용 현재 Access Token */
    void logout(String userId, String deviceId, String accessToken);

    /** 비밀번호 변경 + 모든 기기 강제 로그아웃 */
    void changePassword(String userId, String accessToken, ChangePasswordRequest request);
}
