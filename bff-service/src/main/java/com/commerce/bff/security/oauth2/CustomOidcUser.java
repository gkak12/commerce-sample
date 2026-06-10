package com.commerce.bff.security.oauth2;

import com.commerce.bff.security.CustomUserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Map;

/**
 * OIDC 인증 사용자 래퍼 (Google, Kakao)
 *
 * CustomOAuth2User를 상속하여 OAuth2SuccessHandler의 캐스팅을 그대로 유지하면서
 * Spring Security OIDC 플로우가 요구하는 OidcUser 인터페이스도 구현합니다.
 */
public class CustomOidcUser extends CustomOAuth2User implements OidcUser {

    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    public CustomOidcUser(CustomUserDetails userDetails,
                          Map<String, Object> attributes,
                          OidcIdToken idToken,
                          OidcUserInfo userInfo) {
        super(userDetails, attributes);
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    @Override
    public Map<String, Object> getClaims() {
        return idToken.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }
}
