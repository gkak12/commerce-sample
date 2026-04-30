package com.commerce.bff.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Refresh Token Redis 저장소 (기기별 관리)
 *
 * Key 구조: refresh:token:{userId}:{deviceId}
 * TTL    : jwt.refresh-token-expiration (기본 7일)
 *
 * 기기별 관리:
 *   - 로그인 시 deviceId 발급 → 기기마다 독립적인 Refresh Token
 *   - 특정 기기 로그아웃 시 해당 기기 토큰만 삭제
 *   - 비밀번호 변경 시 모든 기기 토큰 삭제 (deleteAll)
 *
 * Sliding Window Rotation:
 *   - 만료 임박 시 새 토큰 발급 + Redis 갱신
 *   - 이미 교체된 토큰 재사용 → 탈취 감지 → 해당 기기 강제 로그아웃
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
    public void save(String userId, String deviceId, String refreshToken) {
        String key = buildKey(userId, deviceId);
        redisTemplate.opsForValue().set(key, refreshToken,
                Duration.ofMillis(refreshTokenExpirationMs));
        log.debug("[RefreshToken] Saved. userId={}, deviceId={}", userId, deviceId);
    }

    /**
     * Redis 저장값과 일치 여부 검증
     *
     * @return true  → 정상 (일치)
     *         false → 비정상 (불일치 또는 없음 → 탈취 가능성)
     */
    public boolean isValid(String userId, String deviceId, String refreshToken) {
        return find(userId, deviceId)
                .map(stored -> stored.equals(refreshToken))
                .orElse(false);
    }

    /**
     * 특정 기기 Refresh Token 삭제 (해당 기기 로그아웃)
     */
    public void delete(String userId, String deviceId) {
        redisTemplate.delete(buildKey(userId, deviceId));
        log.debug("[RefreshToken] Deleted. userId={}, deviceId={}", userId, deviceId);
    }

    /**
     * 해당 유저의 모든 기기 Refresh Token 삭제 (비밀번호 변경 시 전체 로그아웃)
     *
     * SCAN cursor 방식 사용 — KEYS 명령 사용 금지 (운영 환경 블로킹 위험)
     */
    public void deleteAll(String userId) {
        String pattern = KEY_PREFIX + userId + ":*";
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        List<String> keys = new ArrayList<>();

        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                .getConnection().scan(options)) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
        } catch (Exception e) {
            log.warn("[RefreshToken] SCAN failed. userId={}, error={}", userId, e.getMessage());
            return;
        }

        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("[RefreshToken] All tokens deleted. userId={}, count={}", userId, keys.size());
        }
    }

    private Optional<String> find(String userId, String deviceId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(buildKey(userId, deviceId)));
    }

    private String buildKey(String userId, String deviceId) {
        return KEY_PREFIX + userId + ":" + deviceId;
    }
}
