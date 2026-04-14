package com.commerce.bff.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 설정
 *
 * 접속 URL: http://localhost:8080/swagger-ui/index.html
 *
 * JWT 인증이 필요한 API는 Swagger UI 우측 상단 'Authorize' 버튼 클릭 후
 * "Bearer {토큰값}" 입력
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT 액세스 토큰. /api/auth/login 으로 발급받은 토큰을 입력하세요.");

        return new OpenAPI()
                .info(new Info()
                        .title("Commerce Sample — BFF API")
                        .description("""
                                **Commerce Sample 프로젝트 BFF 서비스 API**

                                | 기능 | 설명 |
                                |------|------|
                                | 인증 | 일반 로그인 / OAuth2 (Google, Naver) |
                                | 주문 | Redis 재고 선점 + Kafka 이벤트 발행 |
                                | 재고 | Redis 기반 실시간 재고 관리 |
                                | 마이페이지 | gRPC 기반 주문/배송/포인트 조회 |
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Commerce Team")
                                .email("commerce@example.com")))
                // 전역 JWT 보안 적용
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerScheme));
    }
}
