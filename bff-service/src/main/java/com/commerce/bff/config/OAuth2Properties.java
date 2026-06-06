package com.commerce.bff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth2")
public class OAuth2Properties {

    /**
     * 소셜 로그인 성공 후 웹 프론트엔드로 리다이렉트할 콜백 URL
     * 예) http://localhost:3000/auth/callback
     */
    private String webRedirectUri;
}
