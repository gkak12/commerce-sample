package com.commerce.bff.service.impl;

import com.commerce.bff.dto.auth.*;
import com.commerce.bff.dto.auth.AdminLoginResult;
import com.commerce.bff.entity.Admin;
import com.commerce.bff.mail.EmailService;
import com.commerce.bff.repository.AdminRepository;
import com.commerce.bff.security.BlacklistTokenService;
import com.commerce.bff.security.DeviceSessionService;
import com.commerce.bff.security.JwtTokenProvider;
import com.commerce.bff.security.RefreshTokenService;
import com.commerce.bff.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthServiceImpl.class);
    private static final String USER_TYPE = "ADMIN";

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final BlacklistTokenService blacklistTokenService;
    private final DeviceSessionService deviceSessionService;
    private final EmailService emailService;

    // ── 관리자 계정 생성 ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public void createAdmin(AdminCreateRequest request, String createdByAdminId) {
        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Already registered email: " + request.getEmail());
        }

        // 임시 비밀번호 생성 (12자리 랜덤)
        String tempPassword = generateTempPassword();

        adminRepository.save(Admin.builder()
                .adminId(UUID.randomUUID().toString())
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(tempPassword))
                .passwordChangeRequired(true)
                .createdBy(createdByAdminId)
                .build());

        // 임시 비밀번호 이메일 발송 (비동기)
        emailService.sendAdminTempPasswordMail(request.getEmail(), request.getName(), tempPassword);

        log.info("[Admin] Account created. email={}, createdBy={}", request.getEmail(), createdByAdminId);
    }

    // ── 로그인 ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AdminLoginResult login(LoginRequest request, String ip) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String deviceId     = UUID.randomUUID().toString();
        String accessToken  = jwtTokenProvider.generateAccessToken(
                admin.getAdminId(), admin.getEmail(), "ROLE_ADMIN", deviceId, USER_TYPE);
        String refreshToken = jwtTokenProvider.generateRefreshToken(admin.getAdminId(), deviceId);

        String evictedDeviceId = deviceSessionService.addDevice(admin.getAdminId(), deviceId);
        if (evictedDeviceId != null) {
            refreshTokenService.delete(admin.getAdminId(), evictedDeviceId);
        }
        refreshTokenService.save(admin.getAdminId(), deviceId, refreshToken);

        log.info("[Admin] Login complete. adminId={}, passwordChangeRequired={}",
                admin.getAdminId(), admin.isPasswordChangeRequired());
        return new AdminLoginResult(AuthTokens.ofNew(accessToken, refreshToken, deviceId),
                admin.isPasswordChangeRequired());
    }

    // ── 토큰 갱신 ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthTokens refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String adminId  = jwtTokenProvider.getUserId(refreshToken);
        String deviceId = jwtTokenProvider.getDeviceId(refreshToken);

        if (!refreshTokenService.isValid(adminId, deviceId, refreshToken)) {
            refreshTokenService.delete(adminId, deviceId);
            deviceSessionService.removeDevice(adminId, deviceId);
            log.warn("[Admin] Refresh token reuse detected. adminId={}", adminId);
            throw new BadCredentialsException("Refresh token reuse detected. Please login again.");
        }

        Admin admin = adminRepository.findByAdminId(adminId)
                .orElseThrow(() -> new BadCredentialsException("Admin not found"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                admin.getAdminId(), admin.getEmail(), "ROLE_ADMIN", deviceId, USER_TYPE);

        if (jwtTokenProvider.isRefreshTokenExpiringSoon(refreshToken)) {
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(admin.getAdminId(), deviceId);
            refreshTokenService.save(adminId, deviceId, newRefreshToken);
            return AuthTokens.ofRotated(newAccessToken, newRefreshToken, deviceId);
        }

        return AuthTokens.ofReused(newAccessToken, refreshToken, deviceId);
    }

    // ── 로그아웃 ──────────────────────────────────────────────────────────────

    @Override
    public void logout(String adminId, String accessToken) {
        String deviceId = jwtTokenProvider.getDeviceId(accessToken);
        refreshTokenService.delete(adminId, deviceId);
        deviceSessionService.removeDevice(adminId, deviceId);
        blacklistTokenService.add(accessToken, jwtTokenProvider.getRemainingMs(accessToken));
        log.info("[Admin] Logout complete. adminId={}, deviceId={}", adminId, deviceId);
    }

    // ── 비밀번호 변경 ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void changePassword(String adminId, String accessToken, ChangePasswordRequest request) {
        Admin admin = adminRepository.findByAdminId(adminId)
                .orElseThrow(() -> new BadCredentialsException("Admin not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // changePassword() 내부에서 passwordChangeRequired = false 처리
        admin.changePassword(passwordEncoder.encode(request.getNewPassword()));
        refreshTokenService.deleteAll(adminId);
        deviceSessionService.removeAll(adminId);
        blacklistTokenService.add(accessToken, jwtTokenProvider.getRemainingMs(accessToken));

        log.info("[Admin] Password changed. All devices logged out. adminId={}", adminId);
    }

    // ── 임시 비밀번호 생성 ────────────────────────────────────────────────────

    private String generateTempPassword() {
        return new BigInteger(60, new SecureRandom()).toString(32);
    }
}
