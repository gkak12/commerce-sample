package com.commerce.bff.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Refresh Token Redis 저장소
 *
 * Key 구조: refresh:token:{userId}
 * TTL    : jwt.refresh-token-expiration (기본 7일)
 *
 * Refresh Token Rotation 전략:
 *   - 토큰 재발급 시 새 Refresh Token 발급 + 기존 토큰 덮어쓰기
 *   - 이미 교체된(폐기된) 토큰으로 재발급 요청 시 → 탈취 감지 → 전체 로그아웃
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final String KEY_PREFIX = "refresh:token:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    /**
     * Refresh Token 저장 (로그인 / 토큰 갱신 시 호출)
     */
    public void save(String userId, String refreshToken) {
        String key = KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken,
                Duration.ofMillis(refreshTokenExpirationMs));
        log.debug("[RefreshToken] Saved. userId={}", userId);
    }

    /**
     * Redis에 저장된 Refresh Token과 일치 여부 검증
     *
     * @return true  → 정상 (Redis 저장값과 일치)
     *         false → 비정상 (저장값 없음 or 불일치 → 탈취 가능성)
     */
    public boolean isValid(String userId, String refreshToken) {
        return find(userId)
                .map(stored -> stored.equals(refreshToken))
                .orElse(false);
    }

    /**
     * Refresh Token 삭제 (로그아웃 / 탈취 감지 시 호출)
     */
    public void delete(String userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
        log.debug("[RefreshToken] Deleted. userId={}", userId);
    }

    private Optional<String> find(String userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + userId));
    }
}
