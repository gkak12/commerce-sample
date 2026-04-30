package com.commerce.bff.service.impl;

import com.commerce.bff.dto.auth.AuthTokens;
import com.commerce.bff.dto.auth.ChangePasswordRequest;
import com.commerce.bff.dto.auth.LoginRequest;
import com.commerce.bff.dto.auth.SignupRequest;
import com.commerce.bff.entity.AuthProvider;
import com.commerce.bff.entity.Role;
import com.commerce.bff.entity.User;
import com.commerce.bff.repository.UserRepository;
import com.commerce.bff.security.BlacklistTokenService;
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
    private final BlacklistTokenService blacklistTokenService;

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

        String deviceId     = UUID.randomUUID().toString();
        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getEmail(), "ROLE_" + user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        refreshTokenService.save(user.getUserId(), deviceId, refreshToken);

        log.info("[Auth] Signup complete. userId={}", user.getUserId());
        return AuthTokens.ofNew(accessToken, refreshToken, deviceId);
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

        String deviceId     = UUID.randomUUID().toString();
        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getEmail(), "ROLE_" + user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        // 기기별 독립 저장 — 다른 기기 토큰에 영향 없음
        refreshTokenService.save(user.getUserId(), deviceId, refreshToken);

        log.info("[Auth] Login complete. userId={}, deviceId={}", user.getUserId(), deviceId);
        return AuthTokens.ofNew(accessToken, refreshToken, deviceId);
    }

    // ── 토큰 갱신 (Sliding Window Rotation) ──────────────────────────────────

    /**
     * Sliding Window Rotation + 탈취 감지 (기기별)
     *
     * 탈취 감지:
     *   이미 Rotation된 토큰 재사용 → 해당 기기 토큰 삭제 → 재로그인 강제
     *   (다른 기기 토큰은 영향 없음)
     */
    @Override
    @Transactional(readOnly = true)
    public AuthTokens refresh(String refreshToken, String deviceId) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String userId = jwtTokenProvider.getUserId(refreshToken);

        // 기기별 탈취 감지
        if (!refreshTokenService.isValid(userId, deviceId, refreshToken)) {
            refreshTokenService.delete(userId, deviceId);
            log.warn("[Auth] Refresh token reuse detected. userId={}, deviceId={}", userId, deviceId);
            throw new BadCredentialsException("Refresh token reuse detected. Please login again.");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getEmail(), "ROLE_" + user.getRole().name());

        // Sliding Window: 만료 임박 시에만 Refresh Token 교체
        if (jwtTokenProvider.isRefreshTokenExpiringSoon(refreshToken)) {
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());
            refreshTokenService.save(userId, deviceId, newRefreshToken);
            log.debug("[Auth] Refresh token rotated. userId={}, deviceId={}", userId, deviceId);
            return AuthTokens.ofRotated(newAccessToken, newRefreshToken, deviceId);
        }

        log.debug("[Auth] Access token refreshed. userId={}, deviceId={}", userId, deviceId);
        return AuthTokens.ofReused(newAccessToken, refreshToken, deviceId);
    }

    // ── 로그아웃 ──────────────────────────────────────────────────────────────

    @Override
    public void logout(String userId, String deviceId, String accessToken) {
        // 1. 해당 기기 Refresh Token 삭제
        refreshTokenService.delete(userId, deviceId);

        // 2. Access Token 블랙리스트 등록 (남은 유효기간 동안 차단)
        long remainingMs = jwtTokenProvider.getRemainingMs(accessToken);
        blacklistTokenService.add(accessToken, remainingMs);

        log.info("[Auth] Logout complete. userId={}, deviceId={}", userId, deviceId);
    }

    // ── 비밀번호 변경 + 전체 기기 로그아웃 ────────────────────────────────────

    /**
     * 비밀번호 변경 후 모든 기기 강제 로그아웃
     *
     * 1. 현재 비밀번호 검증
     * 2. 새 비밀번호로 변경
     * 3. 모든 기기 Refresh Token 삭제 (SCAN cursor)
     * 4. 현재 Access Token 블랙리스트 등록
     */
    @Override
    @Transactional
    public void changePassword(String userId, String accessToken, ChangePasswordRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // 비밀번호 변경
        user.changePassword(passwordEncoder.encode(request.getNewPassword()));

        // 모든 기기 Refresh Token 삭제
        refreshTokenService.deleteAll(userId);

        // 현재 Access Token도 즉시 무효화
        long remainingMs = jwtTokenProvider.getRemainingMs(accessToken);
        blacklistTokenService.add(accessToken, remainingMs);

        log.info("[Auth] Password changed. All devices logged out. userId={}", userId);
    }
}
