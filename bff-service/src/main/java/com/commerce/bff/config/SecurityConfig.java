package com.commerce.bff.config;

import com.commerce.bff.security.BlacklistTokenService;
import com.commerce.bff.security.CustomUserDetailsService;
import com.commerce.bff.security.JwtAuthenticationFilter;
import com.commerce.bff.security.JwtTokenProvider;
import com.commerce.bff.security.oauth2.CustomOAuth2UserService;
import com.commerce.bff.security.oauth2.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // @PreAuthorize, @PostAuthorize 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final BlacklistTokenService blacklistTokenService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── CSRF ────────────────────────────────────────────────────────
            // Spring Security CSRF 비활성화 (JWT Stateless 방식 사용)
            // Access Token  : Authorization 헤더 전송 → 브라우저 자동 첨부 불가 → CSRF 안전
            // Refresh Token : HttpOnly Cookie + SameSite=Strict → 다른 Origin 요청 시 Cookie 미전송
            .csrf(AbstractHttpConfigurer::disable)

            // ── formLogin / httpBasic 비활성화 ──────────────────────────────
            // JWT + REST API 방식 사용 → 세션 기반 로그인 필터 불필요
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            // ── CORS ────────────────────────────────────────────────────────
            // 허용된 Origin만 응답 접근 가능 (application.yml cors.allowed-origins 설정)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── Session ─────────────────────────────────────────────────────
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── 인가 ─────────────────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/signup",
                    "/api/auth/login",
                    "/api/auth/refresh",    // Access Token 만료 상태에서 호출 → 인증 없이 허용
                    "/oauth2/**",
                    "/login/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/actuator/prometheus",
                    "/actuator/health"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/stocks/**").permitAll()
                .requestMatchers("/api/auth/logout").authenticated()
                .anyRequest().authenticated())

            // ── OAuth2 ───────────────────────────────────────────────────────
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo ->
                    userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler))

            // ── JWT Filter ───────────────────────────────────────────────────
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, blacklistTokenService),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정
     *
     * allowedOrigins    : application.yml에서 환경별로 다르게 설정
     * allowedMethods    : REST API에서 사용하는 메서드만 허용
     * allowedHeaders    : Authorization(JWT), Content-Type 허용
     * allowCredentials  : HttpOnly Cookie 전송을 위해 true 필수
     *                     (true 설정 시 allowedOrigins에 * 사용 불가)
     * maxAge            : Preflight 요청 캐시 시간 (1시간)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);   // HttpOnly Cookie 전송 허용
        config.setMaxAge(3600L);            // Preflight 캐시 1시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
