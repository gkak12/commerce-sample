package com.commerce.bff.dto.auth;

/**
 * 서비스 계층 내부 전달용 토큰 쌍
 *
 * - accessToken  : 응답 Body에 포함
 * - refreshToken : HttpOnly Cookie로 설정 (Body에 노출 안 함)
 * - tokenRotated : true면 새 Refresh Token 발급됨 → Cookie 갱신 필요
 */
public record AuthTokens(String accessToken, String refreshToken, boolean tokenRotated) {

    /** 로그인 / 회원가입 — 항상 새 토큰 쌍 */
    public static AuthTokens ofNew(String accessToken, String refreshToken) {
        return new AuthTokens(accessToken, refreshToken, true);
    }

    /** 토큰 갱신 — Refresh Token 재사용 (만료 여유 있음) */
    public static AuthTokens ofReused(String accessToken, String refreshToken) {
        return new AuthTokens(accessToken, refreshToken, false);
    }

    /** 토큰 갱신 — Refresh Token Rotation 발생 (만료 임박) */
    public static AuthTokens ofRotated(String accessToken, String refreshToken) {
        return new AuthTokens(accessToken, refreshToken, true);
    }
}
