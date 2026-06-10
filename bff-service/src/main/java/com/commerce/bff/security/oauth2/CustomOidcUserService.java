package com.commerce.bff.security.oauth2;

import com.commerce.bff.entity.AuthProvider;
import com.commerce.bff.entity.User;
import com.commerce.bff.repository.UserRepository;
import com.commerce.bff.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * OIDC 사용자 서비스 (Google, Kakao)
 *
 * ID Token에서 직접 사용자 정보를 추출합니다.
 * UserInfo API 추가 호출 없이 sub, email, name을 ID Token 클레임에서 파싱합니다.
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOidcUserService.class);

    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider  = AuthProvider.valueOf(registrationId.toUpperCase());

        // ID Token 클레임에서 직접 추출 (UserInfo API 호출 없음)
        String providerId = oidcUser.getSubject();
        String email      = oidcUser.getEmail();
        String name       = resolveName(registrationId, oidcUser);

        User user = saveOrUpdate(email, name, providerId, provider);
        log.info("[OIDC] User logged in. email={}, provider={}", email, registrationId);

        return new CustomOidcUser(
                new CustomUserDetails(user),
                oidcUser.getAttributes(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()
        );
    }

    private User saveOrUpdate(String email, String name, String providerId, AuthProvider provider) {
        return userRepository.findByEmailAndProvider(email, provider)
                .map(user -> {
                    user.updateName(name);
                    return userRepository.save(user);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .userId(UUID.randomUUID().toString())
                        .email(email)
                        .name(name)
                        .provider(provider)
                        .providerId(providerId)
                        .build()));
    }

    /**
     * 제공자별 이름 클레임 추출
     * - Google: name 클레임
     * - Kakao:  nickname 클레임 (profile_nickname 스코프)
     */
    private String resolveName(String registrationId, OidcUser oidcUser) {
        return switch (registrationId) {
            case "kakao" -> {
                Object nickname = oidcUser.getAttribute("nickname");
                yield nickname != null ? (String) nickname : oidcUser.getSubject();
            }
            default -> oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getSubject();
        };
    }
}
