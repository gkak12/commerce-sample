package com.commerce.bff.controller;

import com.commerce.bff.config.CookieProperties;
import com.commerce.bff.config.JwtProperties;
import com.commerce.bff.dto.auth.AuthTokens;
import com.commerce.bff.dto.auth.ChangePasswordRequest;
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
    private static final String DEVICE_ID_HEADER = "X-Device-Id";

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    @Operation(summary = "회원가입",
            description = "이메일/비밀번호로 회원가입. " +
                          "Access Token + deviceId는 Body, Refresh Token은 HttpOnly Cookie로 발급.")
    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@RequestBody SignupRequest request) {
        AuthTokens tokens = authService.signup(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken()).toString())
                .body(TokenResponse.of(tokens.accessToken(), tokens.deviceId()));
    }

    @Operation(summary = "로그인",
            description = "이메일/비밀번호로 로그인. " +
                          "Access Token + deviceId는 Body, Refresh Token은 HttpOnly Cookie로 발급.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken()).toString())
                .body(TokenResponse.of(tokens.accessToken(), tokens.deviceId()));
    }

    @Operation(summary = "토큰 갱신",
            description = "HttpOnly Cookie의 Refresh Token + X-Device-Id 헤더로 새 Access Token 발급. " +
                          "만료 임박 시 Refresh Token도 갱신(Sliding Window Rotation).")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            @RequestHeader(value = DEVICE_ID_HEADER, required = false) String deviceId) {

        if (refreshToken == null || deviceId == null) {
            return ResponseEntity.status(401).build();
        }

        AuthTokens tokens = authService.refresh(refreshToken, deviceId);

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();

        // Rotation 발생 시에만 Cookie 갱신
        if (tokens.tokenRotated()) {
            builder.header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken()).toString());
        }

        return builder.body(TokenResponse.of(tokens.accessToken()));
    }

    @Operation(summary = "로그아웃",
            description = "해당 기기 Refresh Token 삭제 + Access Token 블랙리스트 등록 + Cookie 만료.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = DEVICE_ID_HEADER) String deviceId,
            @RequestHeader("Authorization") String authorization) {

        String accessToken = authorization.substring(7);    // "Bearer " 제거
        authService.logout(userDetails.getUsername(), deviceId, accessToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expireRefreshCookie().toString())
                .build();
    }

    @Operation(summary = "비밀번호 변경",
            description = "비밀번호 변경 후 모든 기기 Refresh Token 삭제 + 현재 Access Token 블랙리스트 등록. " +
                          "모든 기기에서 자동 로그아웃.")
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authorization,
            @RequestBody ChangePasswordRequest request) {

        String accessToken = authorization.substring(7);
        authService.changePassword(userDetails.getUsername(), accessToken, request);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expireRefreshCookie().toString())
                .build();
    }

    // ── Cookie 빌더 ────────────────────────────────────────────────────────────

    private ResponseCookie buildRefreshCookie(String value) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(jwtProperties.getRefreshTokenExpiration() / 1000)
                .build();
    }

    private ResponseCookie expireRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(0)
                .build();
    }
}
