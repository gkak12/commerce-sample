package com.commerce.bff.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Access Token 블랙리스트 (Redis)
 *
 * Key 구조: blacklist:token:{accessToken}
 * TTL    : Access Token 남은 유효기간 (만료 시 Redis에서 자동 삭제)
 *
 * 목적:
 *   로그아웃 시 Access Token을 즉시 무효화
 *   서버는 Access Token을 직접 삭제할 수 없으므로
 *   블랙리스트에 등록하여 이후 요청을 차단
 */
@Service
@RequiredArgsConstructor
public class BlacklistTokenService {

    private static final Logger log = LoggerFactory.getLogger(BlacklistTokenService.class);
    private static final String KEY_PREFIX = "blacklist:token:";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Access Token 블랙리스트 등록
     *
     * @param accessToken 블랙리스트에 등록할 Access Token
     * @param remainingMs Access Token 남은 유효기간 (ms) — TTL로 설정하여 만료 시 자동 삭제
     */
    public void add(String accessToken, long remainingMs) {
        if (remainingMs <= 0) return;  // 이미 만료된 토큰은 등록 불필요
        redisTemplate.opsForValue().set(
                KEY_PREFIX + accessToken, "logout",
                Duration.ofMillis(remainingMs));
        log.debug("[Blacklist] Access token registered. remainingMs={}", remainingMs);
    }

    /**
     * 블랙리스트 등록 여부 확인
     */
    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + accessToken));
    }
}
