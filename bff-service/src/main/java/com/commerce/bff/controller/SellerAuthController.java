package com.commerce.bff.controller;

import com.commerce.bff.config.CookieProperties;
import com.commerce.bff.config.JwtProperties;
import com.commerce.bff.dto.auth.*;
import com.commerce.bff.service.SellerAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "판매자 인증", description = "판매자 회원가입/로그인 API (이메일/비밀번호 전용)")
@RestController
@RequestMapping("/api/auth/seller")
@RequiredArgsConstructor
public class SellerAuthController {

    private static final String REFRESH_TOKEN_COOKIE = "sellerRefreshToken";
    private static final String CLIENT_TYPE_HEADER   = "X-Client-Type";

    private final SellerAuthService sellerAuthService;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    @Operation(summary = "판매자 회원가입", description = "가입 후 사업자 정보를 별도로 제출해야 판매 기능이 활성화됩니다.")
    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(
            @RequestBody @Valid SellerSignupRequest request,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType,
            HttpServletRequest httpRequest) {

        AuthTokens tokens = sellerAuthService.signup(request, extractIp(httpRequest));
        return buildTokenResponse(tokens, clientType);
    }

    @Operation(summary = "판매자 로그인")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequest request,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType,
            HttpServletRequest httpRequest) {

        AuthTokens tokens = sellerAuthService.login(request, extractIp(httpRequest));
        return buildTokenResponse(tokens, clientType);
    }

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest bodyRequest,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType) {

        String refreshToken = resolveRefreshToken(clientType, cookieRefreshToken, bodyRequest);
        if (!StringUtils.hasText(refreshToken)) {
            return ResponseEntity.status(401).build();
        }

        AuthTokens tokens = sellerAuthService.refresh(refreshToken);
        return buildRefreshResponse(tokens, clientType);
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType) {

        sellerAuthService.logout(userDetails.getUsername(), authorization.substring(7));
        return clientType == ClientType.BROWSER
                ? ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expireRefreshCookie().toString()).build()
                : ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 변경")
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType,
            @RequestBody ChangePasswordRequest request) {

        sellerAuthService.changePassword(userDetails.getUsername(), authorization.substring(7), request);
        return clientType == ClientType.BROWSER
                ? ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expireRefreshCookie().toString()).build()
                : ResponseEntity.noContent().build();
    }

    // ── 공통 헬퍼 ─────────────────────────────────────────────────────────────

    private ResponseEntity<TokenResponse> buildTokenResponse(AuthTokens tokens, ClientType clientType) {
        if (clientType == ClientType.BROWSER) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken()).toString())
                    .body(TokenResponse.of(tokens.accessToken(), tokens.deviceId()));
        }
        return ResponseEntity.ok(
                TokenResponse.ofMobile(tokens.accessToken(), tokens.refreshToken(), tokens.deviceId()));
    }

    private ResponseEntity<TokenResponse> buildRefreshResponse(AuthTokens tokens, ClientType clientType) {
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
                .path("/api/auth/seller")
                .maxAge(jwtProperties.getRefreshTokenExpiration() / 1000)
                .build();
    }

    private ResponseCookie expireRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite("Strict")
                .path("/api/auth/seller")
                .maxAge(0)
                .build();
    }
}
