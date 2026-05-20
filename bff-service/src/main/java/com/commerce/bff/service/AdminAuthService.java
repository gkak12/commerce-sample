package com.commerce.bff.service;

import com.commerce.bff.dto.auth.*;
import com.commerce.bff.dto.auth.AdminLoginResult;

/**
 * 관리자 인증 서비스
 * - 이메일/비밀번호 전용 (소셜 로그인 미지원)
 * - 계정 생성은 기존 관리자만 가능 (토큰 발급 없이 생성만)
 */
public interface AdminAuthService {

    /** 관리자 계정 생성 — 임시 비밀번호 발급, 토큰 미발급 */
    void createAdmin(AdminCreateRequest request, String createdByAdminId);

    AdminLoginResult login(LoginRequest request, String ip);

    AuthTokens refresh(String refreshToken);

    void logout(String adminId, String accessToken);

    void changePassword(String adminId, String accessToken, ChangePasswordRequest request);
}
