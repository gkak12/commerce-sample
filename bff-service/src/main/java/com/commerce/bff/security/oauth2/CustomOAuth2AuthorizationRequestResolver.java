package com.commerce.bff.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * client_type 파라미터를 state에 인코딩하는 커스텀 OAuth2 요청 resolver
 *
 * 웹:    /login/oauth2/authorization/google?client_type=WEB
 * 모바일: /login/oauth2/authorization/google?client_type=MOBILE
 *
 * state 형식: {originalState}:{clientType}  예) "abc123:WEB"
 */
@Component
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String BASE_URI          = "/login/oauth2/authorization";
    private static final String CLIENT_TYPE_PARAM = "client_type";
    private static final String DEFAULT_TYPE      = "WEB";

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, BASE_URI);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customize(request, defaultResolver.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return customize(request, defaultResolver.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest customize(HttpServletRequest request,
                                                  OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) return null;

        String clientType = request.getParameter(CLIENT_TYPE_PARAM);
        if (clientType == null || clientType.isBlank()) {
            clientType = DEFAULT_TYPE;
        }

        // client_type을 state 뒤에 ":"로 구분하여 인코딩
        String customState = authorizationRequest.getState() + ":" + clientType;

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .state(customState)
                .build();
    }
}
