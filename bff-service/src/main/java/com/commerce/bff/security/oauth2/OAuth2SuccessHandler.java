package com.commerce.bff.security.oauth2;

import com.commerce.bff.config.CookieProperties;
import com.commerce.bff.config.JwtProperties;
import com.commerce.bff.config.OAuth2Properties;
import com.commerce.bff.security.DeviceSessionService;
import com.commerce.bff.security.JwtTokenProvider;
import com.commerce.bff.security.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String CLIENT_TYPE_MOBILE   = "MOBILE";

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final DeviceSessionService deviceSessionService;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;
    private final OAuth2Properties oAuth2Properties;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        String role = oAuth2User.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        String deviceId     = UUID.randomUUID().toString();
        String accessToken  = jwtTokenProvider.generateAccessToken(
                oAuth2User.getUserId(), oAuth2User.getEmail(), role, deviceId, "USER");
        String refreshToken = jwtTokenProvider.generateRefreshToken(oAuth2User.getUserId(), deviceId);

        // 기기 수 제한 체크 (전략 A — 초과 시 가장 오래된 기기 자동 로그아웃)
        String evictedDeviceId = deviceSessionService.addDevice(oAuth2User.getUserId(), deviceId);
        if (evictedDeviceId != null) {
            refreshTokenService.delete(oAuth2User.getUserId(), evictedDeviceId);

            // ↓ 강제 로그아웃된 기기에 알림 푸시 발송 위치
            // pushNotificationService.sendForceLogoutAlert(oAuth2User.getUserId(), evictedDeviceId,
            //         "최대 로그인 기기 수 초과로 자동 로그아웃되었습니다.");
        }

        refreshTokenService.save(oAuth2User.getUserId(), deviceId, refreshToken);

        // ↓ 소셜 로그인 알림 푸시 발송 위치
        // pushNotificationService.sendLoginAlert(oAuth2User.getUserId(), deviceId, request.getRemoteAddr());

        // Refresh Token: HttpOnly Cookie (웹/모바일 공통)
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(jwtProperties.getRefreshTokenExpiration() / 1000)
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        log.info("[OAuth2] Token issued. userId={}, deviceId={}, clientType={}",
                oAuth2User.getUserId(), deviceId, resolveClientType(request));

        // state 파라미터에서 client_type 추출 후 응답 분기
        String clientType = resolveClientType(request);

        if (CLIENT_TYPE_MOBILE.equals(clientType)) {
            // 모바일: JSON 응답
            sendJsonResponse(response, accessToken, deviceId);
        } else {
            // 웹: 프론트 콜백 URL로 리다이렉트
            String redirectUrl = oAuth2Properties.getWebRedirectUri()
                    + "?accessToken=" + accessToken
                    + "&deviceId=" + deviceId;
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
    }

    /**
     * state 파라미터 형식: {originalState}:{clientType}
     * CustomOAuth2AuthorizationRequestResolver 에서 인코딩한 값
     */
    private String resolveClientType(HttpServletRequest request) {
        String state = request.getParameter("state");
        if (state != null && state.contains(":")) {
            return state.substring(state.lastIndexOf(":") + 1);
        }
        return "WEB"; // 기본값
    }

    private void sendJsonResponse(HttpServletResponse response,
                                   String accessToken, String deviceId) throws IOException {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("accessToken", accessToken);
        body.put("tokenType", "Bearer");
        body.put("deviceId", deviceId);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
