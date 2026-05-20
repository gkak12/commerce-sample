package com.commerce.bff.controller;

import com.commerce.bff.security.AdminDetailsService;
import com.commerce.bff.security.SellerDetailsService;
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
 * ┌ 공통 처리 항목 ────────────────────────────────────────────────────────────┐
 * │ 1. RedisTemplate MockBean     → RateLimitInterceptor NPE 방지           │
 * │ 2. opsForValue().increment()  → 요청마다 카운터 1 반환 (속도 제한 미초과) │
 * │ 3. SellerDetailsService Mock  → JwtAuthenticationFilter 의존성 해소     │
 * │ 4. AdminDetailsService Mock   → JwtAuthenticationFilter 의존성 해소     │
 * │ 5. @EnableMethodSecurity      → @PreAuthorize 권한 검사 활성화           │
 * └───────────────────────────────────────────────────────────────────────────┘
 */
public abstract class BaseControllerTest {

    /**
     * @EnableMethodSecurity 활성화 전용 내부 설정.
     * SecurityConfig 전체를 import 하면 JwtTokenProvider 등 의존 빈이 없어
     * 컨텍스트 로드가 실패하므로 최소 설정만 사용합니다.
     */
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig { }

    @MockBean
    RedisTemplate<String, String> redisTemplate;

    // JwtAuthenticationFilter 생성자 의존성 — 인증 테이블 분리로 추가됨
    @MockBean
    SellerDetailsService sellerDetailsService;

    @MockBean
    AdminDetailsService adminDetailsService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUpBase() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
    }
}
