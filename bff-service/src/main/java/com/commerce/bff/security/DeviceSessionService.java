package com.commerce.bff.security;

import com.commerce.bff.config.DeviceSessionProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 기기별 로그인 세션 관리 (Redis Sorted Set)
 *
 * Key 구조: device:sessions:{userId}
 * Score   : 로그인 시각 (Unix timestamp ms) — 오래된 기기 판별용
 * Member  : deviceId
 *
 * 전략 A (자동 로그아웃):
 *   최대 기기 수 초과 시 가장 오래된 기기(Score 최솟값)를 자동 제거
 *   제거된 deviceId를 반환 → 호출자가 Refresh Token 삭제
 */
@Service
@RequiredArgsConstructor
public class DeviceSessionService {

    private static final Logger log = LoggerFactory.getLogger(DeviceSessionService.class);
    private static final String KEY_PREFIX = "device:sessions:";

    private final RedisTemplate<String, String> redisTemplate;
    private final DeviceSessionProperties deviceSessionProperties;

    /**
     * 신규 기기 등록 + 최대 기기 수 초과 시 가장 오래된 기기 자동 제거 (전략 A)
     *
     * @return 제거된 deviceId (null이면 제거 없음)
     */
    public String addDevice(String userId, String deviceId) {
        String key = KEY_PREFIX + userId;
        double score = System.currentTimeMillis();

        // 신규 기기 등록
        redisTemplate.opsForZSet().add(key, deviceId, score);

        // 최대 기기 수 초과 확인
        Long count = redisTemplate.opsForZSet().size(key);
        if (count != null && count > deviceSessionProperties.getMaxSessions()) {
            // 가장 오래된 기기 (Score 최솟값) 조회 후 제거
            Set<String> oldest = redisTemplate.opsForZSet().range(key, 0, 0);
            if (oldest != null && !oldest.isEmpty()) {
                String evictedDeviceId = oldest.iterator().next();
                redisTemplate.opsForZSet().remove(key, evictedDeviceId);
                log.info("[Device] Oldest device evicted. userId={}, evictedDeviceId={}", userId, evictedDeviceId);
                return evictedDeviceId;
            }
        }

        return null;
    }

    /**
     * 특정 기기 세션 제거 (로그아웃)
     */
    public void removeDevice(String userId, String deviceId) {
        redisTemplate.opsForZSet().remove(KEY_PREFIX + userId, deviceId);
        log.debug("[Device] Device removed. userId={}, deviceId={}", userId, deviceId);
    }

    /**
     * 모든 기기 세션 제거 (비밀번호 변경 시 전체 로그아웃)
     */
    public void removeAll(String userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
        log.info("[Device] All device sessions removed. userId={}", userId);
    }
}
