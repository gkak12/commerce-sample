package com.commerce.bff.dto.auth;

/**
 * 클라이언트 유형
 *
 * X-Client-Type 헤더로 전달
 *   BROWSER : 웹 브라우저 (PC / 태블릿 웹)
 *             → Refresh Token: HttpOnly Cookie
 *   MOBILE  : 네이티브 앱 (Android / iOS)
 *             → Refresh Token: JSON Body
 *                클라이언트가 기기 보안 저장소에 직접 저장
 *                iOS     → Keychain
 *                Android → EncryptedSharedPreferences
 */
public enum ClientType {
    BROWSER, MOBILE
}
