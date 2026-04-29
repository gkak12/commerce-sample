package com.commerce.bff.service.impl;

import com.commerce.bff.dto.auth.AuthTokens;
import com.commerce.bff.dto.auth.LoginRequest;
import com.commerce.bff.dto.auth.SignupRequest;
import com.commerce.bff.entity.AuthProvider;
import com.commerce.bff.entity.Role;
import com.commerce.bff.entity.User;
import com.commerce.bff.repository.UserRepository;
import com.commerce.bff.security.JwtTokenProvider;
import com.commerce.bff.security.RefreshTokenService;
import com.commerce.bff.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    // ── 회원가입 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthTokens signup(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Already registered email: " + request.getEmail());
        }

        User user = userRepository.save(User.builder()
                .userId(UUID.randomUUID().toString())
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .role(Role.USER)
                .build());

        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getEmail(), "ROLE_" + user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        refreshTokenService.save(user.getUserId(), refreshToken);

        log.info("[Auth] Signup complete. userId={}", user.getUserId());
        return AuthTokens.ofNew(accessToken, refreshToken);
    }

    // ── 로그인 ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthTokens login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .filter(u -> u.getProvider() == AuthProvider.LOCAL)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getEmail(), "ROLE_" + user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        // 기존 토큰 덮어쓰기 → 중복 로그인 시 최신 기기만 유효
        refreshTokenService.save(user.getUserId(), refreshToken);

        log.info("[Auth] Login complete. userId={}", user.getUserId());
        return AuthTokens.ofNew(accessToken, refreshToken);
    }

    // ── 토큰 갱신 (Sliding Window Rotation) ──────────────────────────────────

    /**
     * Sliding Window Rotation + 탈취 감지
     *
     * 정상 흐름:
     *   1. JWT 서명/만료 검증
     *   2. Redis 저장값과 일치 여부 검증 (탈취 감지)
     *   3. 새 Access Token 발급 (항상)
     *   4. Refresh Token 만료 임박 여부 판단
     *      ├─ 임박 (1일 미만): 새 Refresh Token 발급 + Redis 갱신 → Cookie 갱신
     *      └─ 여유 있음: 기존 Refresh Token 재사용 → Cookie 변경 없음
     *
     * 탈취 감지:
     *   이미 Rotation된 토큰 재사용 → Redis 전체 삭제 → 재로그인 강제
     */
    @Override
    @Transactional(readOnly = true)
    public AuthTokens refresh(String refreshToken) {
        // 1. JWT 서명/만료 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String userId = jwtTokenProvider.getUserId(refreshToken);

        // 2. Redis 저장값 검증 (탈취 감지)
        if (!refreshTokenService.isValid(userId, refreshToken)) {
            refreshTokenService.delete(userId);
            log.warn("[Auth] Refresh token reuse detected. Force logout. userId={}", userId);
            throw new BadCredentialsException("Refresh token reuse detected. Please login again.");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // 3. 새 Access Token 발급 (항상)
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getEmail(), "ROLE_" + user.getRole().name());

        // 4. Sliding Window: 만료 임박 시에만 Refresh Token 교체
        if (jwtTokenProvider.isRefreshTokenExpiringSoon(refreshToken)) {
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());
            refreshTokenService.save(userId, newRefreshToken);
            log.debug("[Auth] Refresh token rotated (expiring soon). userId={}", userId);
            return AuthTokens.ofRotated(newAccessToken, newRefreshToken);
        }

        log.debug("[Auth] Access token refreshed. Refresh token reused. userId={}", userId);
        return AuthTokens.ofReused(newAccessToken, refreshToken);
    }

    // ── 로그아웃 ──────────────────────────────────────────────────────────────

    @Override
    public void logout(String userId) {
        refreshTokenService.delete(userId);
        log.info("[Auth] Logout complete. userId={}", userId);
    }
}
