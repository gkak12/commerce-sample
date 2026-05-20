package com.commerce.bff.controller;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 컨트롤러 슬라이스 테스트 공통 베이스
 *
 * 모든 @WebMvcTest에서 반복되는 설정을 한 곳에서 관리합니다.
 *
 * ┌ 공통 처리 항목 ──────────────────────────────────────────────────────────┐
 * │ 1. RedisTemplate MockBean  → RateLimitInterceptor NPE 방지             │
 * │ 2. opsForValue().increment() 스터빙  → 요청마다 카운터 1 반환          │
 * │ 3. @EnableMethodSecurity   → @PreAuthorize 권한 검사 활성화            │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
public abstract class BaseControllerTest {

    /**
     * @EnableMethodSecurity 활성화 전용 내부 설정.
     * SecurityConfig 전체를 import하면 JwtTokenProvider 등 의존 빈이 없어
     * 컨텍스트 로드가 실패하므로 최소 설정만 사용합니다.
     */
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig { }

    @MockBean
    RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUpBase() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
    }
}
