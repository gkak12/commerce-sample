package com.commerce.order.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Shedlock 설정 — 분산 환경 스케줄러 중복 실행 방지
 *
 * 문제: 인스턴스 여러 개 실행 시 @Scheduled 가 각 인스턴스에서 동시 실행
 * 해결: DB(shedlock 테이블)에 락을 걸어 한 인스턴스만 실행
 *
 * defaultLockAtMostFor: 락을 최대로 유지하는 시간
 *   → 인스턴스가 비정상 종료되어 락을 반납 못해도 이 시간 후 자동 해제
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10S")
public class ShedlockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()  // 각 인스턴스의 로컬 시계 대신 DB 시간 사용 (시간 편차 방지)
                        .build()
        );
    }
}
