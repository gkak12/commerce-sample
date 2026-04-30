package com.commerce.bff.controller;

import com.commerce.bff.config.CookieProperties;
import com.commerce.bff.config.JwtProperties;
import com.commerce.bff.dto.auth.*;
import com.commerce.bff.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원가입/로그인", description = "회원가입/로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String DEVICE_ID_HEADER    = "X-Device-Id";
    private static final String CLIENT_TYPE_HEADER  = "X-Client-Type";

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    @Operation(summary = "회원가입",
            description = "BROWSER: Refresh Token → HttpOnly Cookie | MOBILE: Refresh Token → JSON Body")
    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(
            @RequestBody SignupRequest request,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType,
            HttpServletRequest httpRequest) {

        AuthTokens tokens = authService.signup(request, extractIp(httpRequest));
        return buildLoginResponse(tokens, clientType);
    }

    @Operation(summary = "로그인",
            description = "BROWSER: Refresh Token → HttpOnly Cookie | MOBILE: Refresh Token → JSON Body")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequest request,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType,
            HttpServletRequest httpRequest) {

        AuthTokens tokens = authService.login(request, extractIp(httpRequest));
        return buildLoginResponse(tokens, clientType);
    }

    @Operation(summary = "토큰 갱신",
            description = "BROWSER: Refresh Token Cookie 자동 전송 | MOBILE: Body의 refreshToken 사용")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest bodyRequest,
            @RequestHeader(value = DEVICE_ID_HEADER, required = false) String deviceId,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType) {

        // 클라이언트 유형에 따라 Refresh Token 추출
        String refreshToken = clientType == ClientType.BROWSER
                ? cookieRefreshToken
                : (bodyRequest != null ? bodyRequest.getRefreshToken() : null);

        if (!StringUtils.hasText(refreshToken) || !StringUtils.hasText(deviceId)) {
            return ResponseEntity.status(401).build();
        }

        AuthTokens tokens = authService.refresh(refreshToken, deviceId);

        if (clientType == ClientType.BROWSER) {
            ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
            if (tokens.tokenRotated()) {
                builder.header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken()).toString());
            }
            return builder.body(TokenResponse.of(tokens.accessToken()));
        } else {
            // MOBILE: Rotation 발생 시 새 Refresh Token도 Body에 포함
            TokenResponse body = tokens.tokenRotated()
                    ? TokenResponse.ofMobileRefresh(tokens.accessToken(), tokens.refreshToken())
                    : TokenResponse.of(tokens.accessToken());
            return ResponseEntity.ok(body);
        }
    }

    @Operation(summary = "로그아웃",
            description = "해당 기기 Refresh Token 삭제 + Access Token 블랙리스트 등록")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = DEVICE_ID_HEADER) String deviceId,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType) {

        String accessToken = authorization.substring(7);
        authService.logout(userDetails.getUsername(), deviceId, accessToken);

        // BROWSER: Cookie 만료 처리
        if (clientType == ClientType.BROWSER) {
            return ResponseEntity.noContent()
                    .header(HttpHeaders.SET_COOKIE, expireRefreshCookie().toString())
                    .build();
        }

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 변경",
            description = "비밀번호 변경 후 모든 기기 강제 로그아웃")
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = CLIENT_TYPE_HEADER, defaultValue = "BROWSER") ClientType clientType,
            @RequestBody ChangePasswordRequest request) {

        String accessToken = authorization.substring(7);
        authService.changePassword(userDetails.getUsername(), accessToken, request);

        if (clientType == ClientType.BROWSER) {
            return ResponseEntity.noContent()
                    .header(HttpHeaders.SET_COOKIE, expireRefreshCookie().toString())
                    .build();
        }

        return ResponseEntity.noContent().build();
    }

    // ── 공통 로그인/회원가입 응답 빌더 ────────────────────────────────────────

    private ResponseEntity<TokenResponse> buildLoginResponse(AuthTokens tokens, ClientType clientType) {
        if (clientType == ClientType.BROWSER) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken()).toString())
                    .body(TokenResponse.of(tokens.accessToken(), tokens.deviceId()));
        } else {
            // MOBILE: Refresh Token을 Body에 포함
            return ResponseEntity.ok(
                    TokenResponse.ofMobile(tokens.accessToken(), tokens.refreshToken(), tokens.deviceId()));
        }
    }

    // ── IP 추출 ────────────────────────────────────────────────────────────────

    /**
     * 클라이언트 IP 추출
     * X-Forwarded-For: 프록시/로드밸런서 환경에서 실제 클라이언트 IP
     * RemoteAddr     : 직접 연결 시 IP
     */
    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            return ip.split(",")[0].trim();  // 여러 프록시 거친 경우 첫 번째가 원본 IP
        }
        return request.getRemoteAddr();
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
