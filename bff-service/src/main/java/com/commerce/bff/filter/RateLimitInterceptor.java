package com.commerce.bff.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Redis 기반 Rate Limiting 인터셉터
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Fixed Window Counter 알고리즘                               │
 * │                                                             │
 * │  Key: rate:{category}:{identifier}:{epochMinute}            │
 * │  TTL: 70초 (윈도우 만료 후 자동 삭제)                        │
 * │                                                             │
 * │  [인증된 사용자] identifier = userId                         │
 * │  [미인증 요청]   identifier = clientIp (로그인/회원가입 전용) │
 * └─────────────────────────────────────────────────────────────┘
 *
 * 엔드포인트별 제한:
 *   POST /api/orders          → 분당 10회  (주문 생성 - 어뷰징 방지)
 *   DELETE /api/orders/**     → 분당 10회  (주문 취소)
 *   POST /api/auth/login      → 분당 10회  (브루트포스 방지, IP 기준)
 *   POST /api/auth/signup     → 분당  5회  (스팸 가입 방지, IP 기준)
 *   GET  /api/my/**           → 분당 60회  (마이페이지 조회)
 *   그 외 인증 필요 API       → 분당 60회  (기본)
 *
 * 응답 헤더:
 *   X-RateLimit-Limit     : 해당 엔드포인트의 분당 허용 횟수
 *   X-RateLimit-Remaining : 현재 윈도우에서 남은 요청 수
 *   Retry-After           : 한도 초과 시 재시도까지 남은 초 (429 응답에만 포함)
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final Duration WINDOW_TTL = Duration.ofSeconds(70); // 윈도우 + 여유

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rate-limit.order-per-minute:10}")
    private int orderLimitPerMinute;

    @Value("${rate-limit.auth-login-per-minute:10}")
    private int loginLimitPerMinute;

    @Value("${rate-limit.auth-signup-per-minute:5}")
    private int signupLimitPerMinute;

    @Value("${rate-limit.default-per-minute:60}")
    private int defaultLimitPerMinute;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {

        String path   = request.getRequestURI();
        String method = request.getMethod();

        // ── 카테고리 & 제한 횟수 결정 ────────────────────────────────────────
        RateLimitRule rule = resolveRule(path, method);

        // ── identifier 결정 (인증 사용자: userId / 미인증: IP) ─────────────
        String identifier = resolveIdentifier(request, rule);
        if (identifier == null) {
            return true; // 인증 필요 API인데 미인증 → Security에서 처리
        }

        // ── Redis fixed window 카운터 ────────────────────────────────────────
        long epochMinute = System.currentTimeMillis() / 60_000;
        String key = "rate:" + rule.category + ":" + identifier + ":" + epochMinute;

        long currentCount;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, WINDOW_TTL);
            }
            currentCount = count == null ? 1L : count;
        } catch (DataAccessException e) {
            // Fail Open: Redis 장애 시 요청 허용 (서비스 가용성 우선)
            // Rate Limit 기능이 일시적으로 비활성화되더라도 정상 서비스가 더 중요
            log.warn("[RateLimit] Redis 장애로 Rate Limit 우회. identifier={}, error={}",
                    identifier, e.getMessage());
            return true;
        }

        // ── 응답 헤더 설정 ──────────────────────────────────────────────────
        response.setIntHeader("X-RateLimit-Limit", rule.limit);
        response.setIntHeader("X-RateLimit-Remaining",
                (int) Math.max(0, rule.limit - currentCount));

        // ── 한도 초과 → 429 반환 ────────────────────────────────────────────
        if (currentCount > rule.limit) {
            log.warn("[RateLimit] 한도 초과. category={}, identifier={}, count={}/{}",
                    rule.category, identifier, currentCount, rule.limit);
            sendTooManyRequests(response, rule.limit);
            return false;
        }

        return true;
    }

    // ── 규칙 결정 ─────────────────────────────────────────────────────────────

    private RateLimitRule resolveRule(String path, String method) {
        // 주문 생성 / 취소
        if (path.startsWith("/api/orders")) {
            if (HttpMethod.POST.matches(method) || HttpMethod.DELETE.matches(method)) {
                return new RateLimitRule("order", orderLimitPerMinute, false);
            }
        }
        // 로그인 (IP 기반 브루트포스 방지)
        if (path.equals("/api/auth/login") && HttpMethod.POST.matches(method)) {
            return new RateLimitRule("auth-login", loginLimitPerMinute, true);
        }
        // 회원가입 (IP 기반 스팸 방지)
        if (path.equals("/api/auth/signup") && HttpMethod.POST.matches(method)) {
            return new RateLimitRule("auth-signup", signupLimitPerMinute, true);
        }
        // 마이페이지 조회 및 기타 인증 API
        return new RateLimitRule("default", defaultLimitPerMinute, false);
    }

    // ── identifier 결정 ───────────────────────────────────────────────────────

    private String resolveIdentifier(HttpServletRequest request, RateLimitRule rule) {
        if (rule.useIp) {
            return extractClientIp(request);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null; // 미인증 → 통과 (Security가 401 처리)
        }
        return auth.getName();
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim(); // 프록시 체인의 첫 번째 IP
        }
        return request.getRemoteAddr();
    }

    // ── 429 응답 ──────────────────────────────────────────────────────────────

    private void sendTooManyRequests(HttpServletResponse response, int limit) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.setIntHeader("Retry-After", 60);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 429);
        body.put("error", "Too Many Requests");
        body.put("message", "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
        body.put("limitPerMinute", limit);
        body.put("retryAfterSeconds", 60);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    // ── 내부 규칙 ─────────────────────────────────────────────────────────────

    private record RateLimitRule(String category, int limit, boolean useIp) {}
}
