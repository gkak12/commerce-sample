package com.commerce.bff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /**
     * 허용할 Origin 목록
     *   로컬  : http://localhost:3000 (프론트 개발 서버)
     *   운영  : https://commerce.com (실제 도메인)
     */
    private List<String> allowedOrigins;
}
