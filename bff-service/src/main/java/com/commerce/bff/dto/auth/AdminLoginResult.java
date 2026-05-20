package com.commerce.bff.dto.auth;

/**
 * 관리자 로그인 결과
 * - AuthTokens에 임시 비밀번호 변경 필요 여부를 함께 전달
 */
public record AdminLoginResult(AuthTokens tokens, boolean passwordChangeRequired) {
}
