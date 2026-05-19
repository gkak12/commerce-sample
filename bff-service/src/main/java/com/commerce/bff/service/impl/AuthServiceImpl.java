package com.commerce.bff.service.impl;

import com.commerce.bff.dto.auth.*;
import com.commerce.bff.entity.AuthProvider;
import com.commerce.bff.entity.Role;
import com.commerce.bff.entity.User;
import com.commerce.bff.repository.UserRepository;
import com.commerce.bff.security.BlacklistTokenService;
import com.commerce.bff.security.DeviceSessionService;
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
    private final DeviceSessionService deviceSessionService;

    // ── 회원가입 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthTokens signup(SignupRequest request, String ip) {
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
                user.getUserId(), user.getEmail(), "ROLE_" + user.getRole().name(), deviceId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId(), deviceId);

        // 기기 세션 등록
        deviceSessionService.addDevice(user.getUserId(), deviceId);
        refreshTokenService.save(user.getUserId(), deviceId, refreshToken);

        // ↓ 회원가입 완료 알림 푸시 발송 위치
        // pushNotificationService.sendSignupAlert(user.getUserId(), ip);

        log.info("[Auth] Signup complete. userId={}", user.getUserId());
        return AuthTokens.ofNew(accessToken, refreshToken, deviceId);
    }

    @Override
    @Transactional
    public void createAdminAccount(AdminCreateRequest request, String ip) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Already registered email: " + request.getEmail());
        }

        User user = userRepository.save(User.builder()
                .userId(UUID.randomUUID().toString())
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .role(Role.ADMIN)
                .build());

        log.info("[Auth] CreateAdminAccount complete. userId={}", user.getUserId());
    }

    // ── 로그인 ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthTokens login(LoginRequest request, String ip) {
        User user = userRepository.findByEmail(request.getEmail())
                .filter(u -> u.getProvider() == AuthProvider.LOCAL)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String deviceId     = UUID.randomUUID().toString();
        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getEmail(), "ROLE_" + user.getRole().name(), deviceId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId(), deviceId);

        // 기기 수 제한 체크 (전략 A — 초과 시 가장 오래된 기기 자동 로그아웃)
        String evictedDeviceId = deviceSessionService.addDevice(user.getUserId(), deviceId);
        if (evictedDeviceId != null) {
            // 강제 로그아웃된 기기의 Refresh Token 삭제
            refreshTokenService.delete(user.getUserId(), evictedDeviceId);

            // ↓ 강제 로그아웃된 기기에 알림 푸시 발송 위치
            // pushNotificationService.sendForceLogoutAlert(user.getUserId(), evictedDeviceId,
            //         "최대 로그인 기기 수 초과로 자동 로그아웃되었습니다.");
        }

        refreshTokenService.save(user.getUserId(), deviceId, refreshToken);

        // ↓ 새 기기 로그인 알림 푸시 발송 위치
        // pushNotificationService.sendLoginAlert(
        //         user.getUserId(),   // 알림 수신 대상
        //         deviceId,           // 로그인한 기기
        //         ip                  // 로그인 IP (위치 표시용)
        // );

        log.info("[Auth] Login complete. userId={}, deviceId={}", user.getUserId(), deviceId);
        return AuthTokens.ofNew(accessToken, refreshToken, deviceId);
    }

    // ── 토큰 갱신 (Sliding Window Rotation) ──────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthTokens refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String userId   = jwtTokenProvider.getUserId(refreshToken);
        String deviceId = jwtTokenProvider.getDeviceId(refreshToken);

        if (!refreshTokenService.isValid(userId, deviceId, refreshToken)) {
            refreshTokenService.delete(userId, deviceId);
            deviceSessionService.removeDevice(userId, deviceId);
            log.warn("[Auth] Refresh token reuse detected. userId={}, deviceId={}", userId, deviceId);

            // ↓ 탈취 감지 알림 푸시 발송 위치
            // pushNotificationService.sendTokenTheftAlert(userId, deviceId);

            throw new BadCredentialsException("Refresh token reuse detected. Please login again.");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getEmail(), "ROLE_" + user.getRole().name(), deviceId);

        if (jwtTokenProvider.isRefreshTokenExpiringSoon(refreshToken)) {
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId(), deviceId);
            refreshTokenService.save(userId, deviceId, newRefreshToken);
            log.debug("[Auth] Refresh token rotated. userId={}, deviceId={}", userId, deviceId);
            return AuthTokens.ofRotated(newAccessToken, newRefreshToken, deviceId);
        }

        log.debug("[Auth] Access token refreshed. userId={}, deviceId={}", userId, deviceId);
        return AuthTokens.ofReused(newAccessToken, refreshToken, deviceId);
    }

    // ── 로그아웃 ──────────────────────────────────────────────────────────────

    @Override
    public void logout(String userId, String accessToken) {
        // Access Token 클레임에서 deviceId 추출
        String deviceId = jwtTokenProvider.getDeviceId(accessToken);

        // 해당 기기 Refresh Token + 세션 삭제
        refreshTokenService.delete(userId, deviceId);
        deviceSessionService.removeDevice(userId, deviceId);

        // Access Token 블랙리스트 등록 (즉시 무효화)
        long remainingMs = jwtTokenProvider.getRemainingMs(accessToken);
        blacklistTokenService.add(accessToken, remainingMs);

        log.info("[Auth] Logout complete. userId={}, deviceId={}", userId, deviceId);
    }

    // ── 비밀번호 변경 + 전체 기기 로그아웃 ────────────────────────────────────

    @Override
    @Transactional
    public void changePassword(String userId, String accessToken, ChangePasswordRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));

        // 모든 기기 Refresh Token + 세션 삭제
        refreshTokenService.deleteAll(userId);
        deviceSessionService.removeAll(userId);

        // 현재 Access Token 즉시 무효화
        long remainingMs = jwtTokenProvider.getRemainingMs(accessToken);
        blacklistTokenService.add(accessToken, remainingMs);

        // ↓ 비밀번호 변경 알림 푸시 발송 위치 (모든 기기에 알림)
        // pushNotificationService.sendPasswordChangedAlert(userId);

        log.info("[Auth] Password changed. All devices logged out. userId={}", userId);
    }
}
