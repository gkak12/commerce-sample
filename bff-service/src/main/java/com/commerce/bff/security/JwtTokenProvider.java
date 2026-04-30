package com.commerce.bff.security;

import com.commerce.bff.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId, String email, String role) {
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] Token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("[JWT] Unsupported token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("[JWT] Malformed token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("[JWT] Invalid token: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Refresh Token 만료 임박 여부 확인 (Sliding Window Rotation 판단용)
     *
     * 남은 유효기간이 rotationThreshold 미만이면 true → 새 Refresh Token 발급
     * 남은 유효기간이 충분하면 false → 기존 Refresh Token 재사용 (Redis WRITE 생략)
     */
    public boolean isRefreshTokenExpiringSoon(String token) {
        Date expiration = parseClaims(token).getExpiration();
        long remainingMs = expiration.getTime() - System.currentTimeMillis();
        return remainingMs < Duration.ofDays(jwtProperties.getRotationThresholdDays()).toMillis();
    }

    /**
     * Access Token 남은 유효기간 (ms) 반환
     * 블랙리스트 TTL 설정에 사용 — 토큰 만료 시 Redis에서 자동 삭제
     */
    public long getRemainingMs(String token) {
        return parseClaims(token).getExpiration().getTime() - System.currentTimeMillis();
    }
}
