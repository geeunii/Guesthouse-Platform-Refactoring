package com.ssg9th2team.geharbang.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testcontainers + Flyway 통합 테스트 예시
 *
 * 이 테스트는 실제 MySQL 컨테이너에서 실행됩니다.
 * IntegrationTestConfig를 상속받으면 자동으로 Testcontainers가 설정됩니다.
 */
class SampleIntegrationTest extends IntegrationTestConfig {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("MySQL 컨테이너 연결 및 Flyway 마이그레이션 확인")
    void testDatabaseConnection() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Flyway 히스토리 테이블 확인
            ResultSet rs = statement.executeQuery(
                "SELECT COUNT(*) FROM flyway_schema_history"
            );

            assertTrue(rs.next());
            int migrationCount = rs.getInt(1);
            assertTrue(migrationCount > 0, "Flyway 마이그레이션이 실행되어야 합니다");

            System.out.println("적용된 마이그레이션 수: " + migrationCount);
        }
    }

    @Test
    @DisplayName("users 테이블 존재 확인")
    void testUsersTableExists() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            ResultSet rs = statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = 'guesthouse_test' AND table_name = 'users'"
            );

            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "users 테이블이 존재해야 합니다");
        }
    }
}
