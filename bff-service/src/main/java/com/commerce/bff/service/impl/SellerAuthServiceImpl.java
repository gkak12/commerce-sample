package com.commerce.bff.service.impl;

import com.commerce.bff.dto.auth.*;
import com.commerce.bff.entity.Seller;
import com.commerce.bff.repository.SellerRepository;
import com.commerce.bff.security.BlacklistTokenService;
import com.commerce.bff.security.DeviceSessionService;
import com.commerce.bff.security.JwtTokenProvider;
import com.commerce.bff.security.RefreshTokenService;
import com.commerce.bff.service.SellerAuthService;
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
public class SellerAuthServiceImpl implements SellerAuthService {

    private static final Logger log = LoggerFactory.getLogger(SellerAuthServiceImpl.class);
    private static final String USER_TYPE = "SELLER";

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final BlacklistTokenService blacklistTokenService;
    private final DeviceSessionService deviceSessionService;

    // ── 회원가입 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthTokens signup(SellerSignupRequest request, String ip) {
        if (sellerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Already registered email: " + request.getEmail());
        }

        Seller seller = sellerRepository.save(Seller.builder()
                .sellerId(UUID.randomUUID().toString())
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .build());

        log.info("[Seller] Signup complete. sellerId={}", seller.getSellerId());
        return issueTokens(seller.getSellerId(), seller.getEmail());
    }

    // ── 로그인 ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthTokens login(LoginRequest request, String ip) {
        Seller seller = sellerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), seller.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String deviceId     = UUID.randomUUID().toString();
        String accessToken  = jwtTokenProvider.generateAccessToken(
                seller.getSellerId(), seller.getEmail(), "ROLE_SELLER", deviceId, USER_TYPE);
        String refreshToken = jwtTokenProvider.generateRefreshToken(seller.getSellerId(), deviceId);

        String evictedDeviceId = deviceSessionService.addDevice(seller.getSellerId(), deviceId);
        if (evictedDeviceId != null) {
            refreshTokenService.delete(seller.getSellerId(), evictedDeviceId);
        }
        refreshTokenService.save(seller.getSellerId(), deviceId, refreshToken);

        log.info("[Seller] Login complete. sellerId={}, deviceId={}", seller.getSellerId(), deviceId);
        return AuthTokens.ofNew(accessToken, refreshToken, deviceId);
    }

    // ── 토큰 갱신 ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthTokens refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String sellerId = jwtTokenProvider.getUserId(refreshToken);
        String deviceId = jwtTokenProvider.getDeviceId(refreshToken);

        if (!refreshTokenService.isValid(sellerId, deviceId, refreshToken)) {
            refreshTokenService.delete(sellerId, deviceId);
            deviceSessionService.removeDevice(sellerId, deviceId);
            log.warn("[Seller] Refresh token reuse detected. sellerId={}", sellerId);
            throw new BadCredentialsException("Refresh token reuse detected. Please login again.");
        }

        Seller seller = sellerRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new BadCredentialsException("Seller not found"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                seller.getSellerId(), seller.getEmail(), "ROLE_SELLER", deviceId, USER_TYPE);

        if (jwtTokenProvider.isRefreshTokenExpiringSoon(refreshToken)) {
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(seller.getSellerId(), deviceId);
            refreshTokenService.save(sellerId, deviceId, newRefreshToken);
            return AuthTokens.ofRotated(newAccessToken, newRefreshToken, deviceId);
        }

        return AuthTokens.ofReused(newAccessToken, refreshToken, deviceId);
    }

    // ── 로그아웃 ──────────────────────────────────────────────────────────────

    @Override
    public void logout(String sellerId, String accessToken) {
        String deviceId = jwtTokenProvider.getDeviceId(accessToken);
        refreshTokenService.delete(sellerId, deviceId);
        deviceSessionService.removeDevice(sellerId, deviceId);
        blacklistTokenService.add(accessToken, jwtTokenProvider.getRemainingMs(accessToken));
        log.info("[Seller] Logout complete. sellerId={}, deviceId={}", sellerId, deviceId);
    }

    // ── 비밀번호 변경 ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void changePassword(String sellerId, String accessToken, ChangePasswordRequest request) {
        Seller seller = sellerRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new BadCredentialsException("Seller not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), seller.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        seller.changePassword(passwordEncoder.encode(request.getNewPassword()));
        refreshTokenService.deleteAll(sellerId);
        deviceSessionService.removeAll(sellerId);
        blacklistTokenService.add(accessToken, jwtTokenProvider.getRemainingMs(accessToken));

        log.info("[Seller] Password changed. All devices logged out. sellerId={}", sellerId);
    }

    // ── 내부 공통 토큰 발급 ───────────────────────────────────────────────────

    private AuthTokens issueTokens(String sellerId, String email) {
        String deviceId     = UUID.randomUUID().toString();
        String accessToken  = jwtTokenProvider.generateAccessToken(
                sellerId, email, "ROLE_SELLER", deviceId, USER_TYPE);
        String refreshToken = jwtTokenProvider.generateRefreshToken(sellerId, deviceId);

        deviceSessionService.addDevice(sellerId, deviceId);
        refreshTokenService.save(sellerId, deviceId, refreshToken);

        return AuthTokens.ofNew(accessToken, refreshToken, deviceId);
    }
}
