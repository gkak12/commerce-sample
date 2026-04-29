package com.commerce.bff.controller;

import com.commerce.bff.config.CookieProperties;
import com.commerce.bff.config.JwtProperties;
import com.commerce.bff.dto.auth.AuthTokens;
import com.commerce.bff.dto.auth.LoginRequest;
import com.commerce.bff.dto.auth.SignupRequest;
import com.commerce.bff.dto.auth.TokenResponse;
import com.commerce.bff.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원가입/로그인", description = "회원가입/로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    @Operation(summary = "회원가입",
            description = "이메일/비밀번호로 회원가입. Access Token은 Body, Refresh Token은 HttpOnly Cookie로 발급.")
    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@RequestBody SignupRequest request) {
        AuthTokens tokens = authService.signup(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken()).toString())
                .body(TokenResponse.of(tokens.accessToken()));
    }

    @Operation(summary = "로그인",
            description = "이메일/비밀번호로 로그인. Access Token은 Body, Refresh Token은 HttpOnly Cookie로 발급.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken()).toString())
                .body(TokenResponse.of(tokens.accessToken()));
    }

    @Operation(summary = "토큰 갱신",
            description = "HttpOnly Cookie의 Refresh Token으로 새 Access Token 발급. " +
                          "만료 임박 시 Refresh Token도 갱신(Sliding Window Rotation). " +
                          "탈취된 토큰 재사용 감지 시 강제 로그아웃.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {

        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        AuthTokens tokens = authService.refresh(refreshToken);

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();

        // Rotation 발생 시에만 Cookie 갱신 (Sliding Window — 만료 임박 시만)
        if (tokens.tokenRotated()) {
            builder.header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken()).toString());
        }

        return builder.body(TokenResponse.of(tokens.accessToken()));
    }

    @Operation(summary = "로그아웃",
            description = "Redis에서 Refresh Token 삭제 + Cookie 만료 처리.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expireRefreshCookie().toString())
                .build();
    }

    // ── Cookie 빌더 ────────────────────────────────────────────────────────────

    /**
     * Refresh Token HttpOnly Cookie 생성
     *
     * HttpOnly  : JavaScript 접근 차단 (XSS 방어)
     * Secure    : HTTPS 전송만 허용 (운영 환경에서 true 권장)
     * SameSite  : Strict — 다른 도메인 요청 시 Cookie 미전송 (CSRF 방어)
     * Path      : /api/auth — 인증 엔드포인트에만 Cookie 전송
     * MaxAge    : Refresh Token 만료 시간과 동일
     */
    private ResponseCookie buildRefreshCookie(String value) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())    // 로컬: false / 운영(prod): true
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(jwtProperties.getRefreshTokenExpiration() / 1000)   // ms → 초
                .build();
    }

    /** 로그아웃 시 Cookie 즉시 만료 */
    private ResponseCookie expireRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())    // 로컬: false / 운영(prod): true
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(0)
                .build();
    }
}
