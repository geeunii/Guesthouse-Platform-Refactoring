package com.ssg9th2team.geharbang.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.ssg9th2team.geharbang.domain.ocr.service.OcrService;

/**
 * Testcontainers + Flyway 기반 통합 테스트 설정
 *
 * 이 클래스를 상속받으면 실제 MySQL 환경에서 테스트가 실행됩니다.
 * Flyway가 자동으로 마이그레이션을 수행합니다.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("integration-test")
@org.springframework.context.annotation.Import(IntegrationTestConfig.TestConfig.class)
public abstract class IntegrationTestConfig {

    // OCR 서비스는 Google Cloud 인증이 필요하므로 Mock 처리
    @MockBean
    protected OcrService ocrService;

    // CacheManager가 없어서 컨텍스트 로드 실패하는 문제 해결 (Redis 제외 시 필요)
    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.cache.CacheManager cacheManager() {
            return new org.springframework.cache.concurrent.ConcurrentMapCacheManager();
        }
    }

    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("guesthouse_test")
            .withUsername("test")
            .withPassword("test")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    static {
        mysql.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // Flyway 설정
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> true);

        // JPA 설정
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
