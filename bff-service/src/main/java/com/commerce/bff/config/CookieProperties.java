package com.commerce.bff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cookie")
public class CookieProperties {

    /**
     * Refresh Token Cookie Secure 플래그
     *   false : HTTP 전송 허용 (로컬 개발환경)
     *   true  : HTTPS 전송만 허용 (운영 환경) — application-prod.yml에서 true로 설정
     */
    private boolean secure;
}
