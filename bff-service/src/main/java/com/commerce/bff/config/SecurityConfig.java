package com.commerce.bff.config;

import com.commerce.bff.security.*;
import com.commerce.bff.security.oauth2.CustomOAuth2AuthorizationRequestResolver;
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
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final SellerDetailsService sellerDetailsService;
    private final AdminDetailsService adminDetailsService;
    private final BlacklistTokenService blacklistTokenService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomOAuth2AuthorizationRequestResolver authorizationRequestResolver;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // ── 일반 회원 인증 ────────────────────────────────────────
                .requestMatchers(
                    "/api/auth/signup",
                    "/api/auth/login",
                    "/api/auth/refresh"
                ).permitAll()
                // ── 판매자 인증 ───────────────────────────────────────────
                .requestMatchers(
                    "/api/auth/seller/signup",
                    "/api/auth/seller/login",
                    "/api/auth/seller/refresh"
                ).permitAll()
                // ── 관리자 인증 ───────────────────────────────────────────
                .requestMatchers(
                    "/api/admin/auth/login",
                    "/api/admin/auth/refresh"
                ).permitAll()
                // ── OAuth2 ────────────────────────────────────────────────
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                // ── 공개 API ──────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,
                    "/api/products",
                    "/api/products/{productId}",
                    "/api/stocks/**"
                ).permitAll()
                // ── 문서/모니터링 ─────────────────────────────────────────
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/actuator/prometheus",
                    "/actuator/health"
                ).permitAll()
                .anyRequest().authenticated())

            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestResolver(authorizationRequestResolver))
                .userInfoEndpoint(userInfo ->
                    userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler))

            .addFilterBefore(
                new JwtAuthenticationFilter(
                        jwtTokenProvider,
                        userDetailsService,
                        sellerDetailsService,
                        adminDetailsService,
                        blacklistTokenService),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Client-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

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
