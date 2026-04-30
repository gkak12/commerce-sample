package com.commerce.bff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "device")
public class DeviceSessionProperties {

    /**
     * 사용자당 최대 동시 로그인 기기 수
     * 초과 시 가장 오래된 기기 자동 로그아웃 (전략 A)
     */
    private int maxSessions = 5;
}
