package com.commerce.bff.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 클라이언트 응답용 토큰 DTO
 *
 * - accessToken  : 메모리에 보관하여 API 요청 시 Authorization 헤더로 전송
 * - deviceId     : 기기 식별자 — 로그인/회원가입 시만 발급, 이후 X-Device-Id 헤더로 전송
 *                  null이면 JSON 응답에서 제외 (@JsonInclude)
 * - Refresh Token: HttpOnly Cookie로 전달 (Body에 포함 안 함)
 */
@Getter
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String deviceId;    // 로그인/회원가입 시만 포함, 토큰 갱신 시 null

    /** 토큰 갱신 응답 — deviceId 없음 (클라이언트가 이미 보관 중) */
    public static TokenResponse of(String accessToken) {
        return new TokenResponse(accessToken, "Bearer", null);
    }

    /** 로그인/회원가입 응답 — deviceId 포함 */
    public static TokenResponse of(String accessToken, String deviceId) {
        return new TokenResponse(accessToken, "Bearer", deviceId);
    }
}
