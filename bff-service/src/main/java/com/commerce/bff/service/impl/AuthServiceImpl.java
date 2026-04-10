package com.commerce.bff.service.impl;

import com.commerce.bff.dto.auth.LoginRequest;
import com.commerce.bff.dto.auth.RefreshTokenRequest;
import com.commerce.bff.dto.auth.SignupRequest;
import com.commerce.bff.dto.auth.TokenResponse;
import com.commerce.bff.entity.AuthProvider;
import com.commerce.bff.entity.Role;
import com.commerce.bff.entity.User;
import com.commerce.bff.repository.UserRepository;
import com.commerce.bff.security.JwtTokenProvider;
import com.commerce.bff.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public TokenResponse signup(SignupRequest request) {
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

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getEmail(), "ROLE_" + user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        return TokenResponse.of(accessToken, refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .filter(u -> u.getProvider() == AuthProvider.LOCAL)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getEmail(), "ROLE_" + user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        return TokenResponse.of(accessToken, refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getEmail(), "ROLE_" + user.getRole().name());

        return TokenResponse.of(newAccessToken, refreshToken);
    }
}
