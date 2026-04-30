package com.commerce.bff.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 클라이언트 응답용 토큰 DTO
 *
 * BROWSER: refreshToken → HttpOnly Cookie, Body에 미포함
 * MOBILE : refreshToken → Body에 포함 (기기 보안 저장소에 직접 저장)
 *
 * @JsonInclude(NON_NULL) : null 필드는 JSON 응답에서 제외
 */
@Getter
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String deviceId;        // 로그인/회원가입 시만 포함

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String refreshToken;    // MOBILE 클라이언트만 포함

    /** BROWSER — 토큰 갱신 응답 */
    public static TokenResponse of(String accessToken) {
        return new TokenResponse(accessToken, "Bearer", null, null);
    }

    /** BROWSER — 로그인/회원가입 응답 */
    public static TokenResponse of(String accessToken, String deviceId) {
        return new TokenResponse(accessToken, "Bearer", deviceId, null);
    }

    /** MOBILE — 로그인/회원가입 응답 (refreshToken Body 포함) */
    public static TokenResponse ofMobile(String accessToken, String refreshToken, String deviceId) {
        return new TokenResponse(accessToken, "Bearer", deviceId, refreshToken);
    }

    /** MOBILE — 토큰 갱신 응답 (Rotation 발생 시 refreshToken 포함) */
    public static TokenResponse ofMobileRefresh(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, "Bearer", null, refreshToken);
    }
}
