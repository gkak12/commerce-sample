package com.commerce.bff.service;

import com.commerce.bff.dto.auth.*;

public interface AuthService {

    /** @param ip 로그인 요청 IP (로그인 알림 푸시용) */
    AuthTokens signup(SignupRequest request, String ip);

    /** @param ip 로그인 요청 IP (로그인 알림 푸시용) */
    void createAdminAccount(AdminCreateRequest request, String ip);

    /** @param ip 로그인 요청 IP (로그인 알림 푸시용) */
    AuthTokens login(LoginRequest request, String ip);

    /** @param refreshToken HttpOnly Cookie 또는 Body에서 추출한 Refresh Token
     *                     (deviceId는 토큰 클레임에서 추출) */
    AuthTokens refresh(String refreshToken);

    /** @param accessToken 블랙리스트 등록용 현재 Access Token
     *                    (deviceId는 accessToken 클레임에서 추출) */
    void logout(String userId, String accessToken);

    /** 비밀번호 변경 + 모든 기기 강제 로그아웃 */
    void changePassword(String userId, String accessToken, ChangePasswordRequest request);
}
