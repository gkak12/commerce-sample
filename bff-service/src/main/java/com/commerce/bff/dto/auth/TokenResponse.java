package com.commerce.bff.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 클라이언트 응답용 토큰 DTO
 *
 * Refresh Token은 HttpOnly Cookie로 전달되므로 Body에 포함하지 않음
 */
@Getter
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType;

    public static TokenResponse of(String accessToken) {
        return new TokenResponse(accessToken, "Bearer");
    }
}
