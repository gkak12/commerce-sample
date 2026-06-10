package com.commerce.bff.security.oauth2;

import com.commerce.bff.entity.AuthProvider;
import com.commerce.bff.entity.Role;
import com.commerce.bff.entity.User;
import com.commerce.bff.repository.UserRepository;
import com.commerce.bff.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = resolveUserInfo(registrationId, oAuth2User.getAttributes());

        User user = saveOrUpdate(userInfo, registrationId);
        log.info("[OAuth2] User logged in. email={}, provider={}", user.getEmail(), registrationId);

        return new CustomOAuth2User(new CustomUserDetails(user), oAuth2User.getAttributes());
    }

    private OAuth2UserInfo resolveUserInfo(String registrationId, java.util.Map<String, Object> attributes) {
        return switch (registrationId) {
            case "naver" -> new NaverOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        };
    }

    private User saveOrUpdate(OAuth2UserInfo userInfo, String registrationId) {
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        return userRepository.findByEmailAndProvider(userInfo.getEmail(), provider)
                .map(user -> {
                    user.updateName(userInfo.getName());
                    return userRepository.save(user);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .userId(UUID.randomUUID().toString())
                        .email(userInfo.getEmail())
                        .name(userInfo.getName())
                        .provider(provider)
                        .providerId(userInfo.getProviderId())
                        .build()));
    }
}
