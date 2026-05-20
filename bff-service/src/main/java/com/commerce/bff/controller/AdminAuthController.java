package com.commerce.bff.controller;

import com.commerce.bff.config.CookieProperties;
import com.commerce.bff.config.JwtProperties;
import com.commerce.bff.dto.auth.*;
import com.commerce.bff.dto.auth.AdminLoginResult;
import com.commerce.bff.security.AdminDetails;
import com.commerce.bff.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 인증", description = "관리자 로그인/계정 생성 API")
@RestController
@RequiredArgsConstructor
public class AdminAuthController {

    private static final String REFRESH_TOKEN_COOKIE = "adminRefreshToken";
    private static final String CLIENT_TYPE_HEADER   = "X-Client-Type";

    private final AdminAuthService adminAuthService;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    // ── POST /api/admin/accounts ──────────────────────────────────────────────

    @Operation(summary = "관리자 계정 생성", description = "기존 관리자만 가능. 임시 비밀번호를 이메일로 발송합니다.")
    @PostMapping("/api/admin/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createAdmin(
            @RequestBody @Valid AdminCreateRequest request,
            @AuthenticationPrincipal AdminDetails adminDetails) {

        adminAuthService.createAdmin(request, adminDetails.getAdmin().getAdminId());
        return ResponseEntity.status(201).build();
    }

    // ── POST /api/admin/auth/login ────────────────────────────────────────────

    @Operation(summary = "관리자 로그인", description = "임시 비밀번호 변경이 필요하면 응답에 passwordChangeRequired: true 포함")
    @PostMapping("/api/admin/auth/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequest request,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType,
            HttpServletRequest httpRequest) {

        AdminLoginResult result = adminAuthService.login(request, extractIp(httpRequest));
        AuthTokens tokens = result.tokens();
        boolean pwChangeRequired = result.passwordChangeRequired();

        if (clientType == ClientType.BROWSER) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken()).toString())
                    .body(TokenResponse.ofAdmin(tokens.accessToken(), tokens.deviceId(), pwChangeRequired));
        }
        return ResponseEntity.ok(
                TokenResponse.ofAdminMobile(tokens.accessToken(), tokens.refreshToken(),
                        tokens.deviceId(), pwChangeRequired));
    }

    // ── POST /api/admin/auth/refresh ──────────────────────────────────────────

    @Operation(summary = "토큰 갱신")
    @PostMapping("/api/admin/auth/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest bodyRequest,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType) {

        String refreshToken = resolveRefreshToken(clientType, cookieRefreshToken, bodyRequest);
        if (!StringUtils.hasText(refreshToken)) {
            return ResponseEntity.status(401).build();
        }

        AuthTokens tokens = adminAuthService.refresh(refreshToken);

        if (clientType == ClientType.BROWSER) {
            ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
            if (tokens.tokenRotated()) {
                builder.header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken()).toString());
            }
            return builder.body(TokenResponse.of(tokens.accessToken()));
        }
        TokenResponse body = tokens.tokenRotated()
                ? TokenResponse.ofMobileRefresh(tokens.accessToken(), tokens.refreshToken())
                : TokenResponse.of(tokens.accessToken());
        return ResponseEntity.ok(body);
    }

    // ── POST /api/admin/auth/logout ───────────────────────────────────────────

    @Operation(summary = "로그아웃")
    @PostMapping("/api/admin/auth/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal AdminDetails adminDetails,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType) {

        adminAuthService.logout(adminDetails.getAdmin().getAdminId(), authorization.substring(7));
        return clientType == ClientType.BROWSER
                ? ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expireRefreshCookie().toString()).build()
                : ResponseEntity.noContent().build();
    }

    // ── POST /api/admin/auth/password ─────────────────────────────────────────

    @Operation(summary = "비밀번호 변경", description = "임시 비밀번호 변경 완료 시 passwordChangeRequired가 false로 전환됩니다.")
    @PostMapping("/api/admin/auth/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AdminDetails adminDetails,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType,
            @RequestBody ChangePasswordRequest request) {

        adminAuthService.changePassword(adminDetails.getAdmin().getAdminId(), authorization.substring(7), request);
        return clientType == ClientType.BROWSER
                ? ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expireRefreshCookie().toString()).build()
                : ResponseEntity.noContent().build();
    }

    // ── 공통 헬퍼 ─────────────────────────────────────────────────────────────

    private String resolveRefreshToken(ClientType clientType, String cookie, RefreshTokenRequest body) {
        return clientType == ClientType.BROWSER ? cookie : (body != null ? body.getRefreshToken() : null);
    }

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return StringUtils.hasText(ip) ? ip.split(",")[0].trim() : request.getRemoteAddr();
    }

    private ResponseCookie buildRefreshCookie(String value) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite("Strict")
                .path("/api/admin/auth")
                .maxAge(jwtProperties.getRefreshTokenExpiration() / 1000)
                .build();
    }

    private ResponseCookie expireRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite("Strict")
                .path("/api/admin/auth")
                .maxAge(0)
                .build();
    }
}
