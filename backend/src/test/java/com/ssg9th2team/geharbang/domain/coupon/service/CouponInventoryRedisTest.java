package com.ssg9th2team.geharbang.domain.coupon.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 기반 쿠폰 재고 관리 시스템 검증 테스트
 */
@SpringBootTest
class CouponInventoryRedisTest {

    private static final Logger log = LoggerFactory.getLogger(CouponInventoryRedisTest.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CouponInventoryService couponInventoryService;

    @Test
    @DisplayName("Redis 연결 테스트")
    void redisConnection() {
        // Given
        String testKey = "test:key";
        String testValue = "hello-redis";

        // When
        redisTemplate.opsForValue().set(testKey, testValue);
        String result = redisTemplate.opsForValue().get(testKey);

        // Then
        assertThat(result).isEqualTo(testValue);
        log.info("✅ Redis 연결 성공: {}", result);

        // Clean up
        redisTemplate.delete(testKey);
    }

    @Test
    @DisplayName("Redis DECR 테스트 - 원자적 감소")
    void redis_DECR_atomic() {
        // Given
        String stockKey = "test:stock:atomic";
        redisTemplate.opsForValue().set(stockKey, "10");

        // When
        Long remaining1 = redisTemplate.opsForValue().decrement(stockKey);
        Long remaining2 = redisTemplate.opsForValue().decrement(stockKey);
        Long remaining3 = redisTemplate.opsForValue().decrement(stockKey);

        // Then
        assertThat(remaining1).isEqualTo(9L);
        assertThat(remaining2).isEqualTo(8L);
        assertThat(remaining3).isEqualTo(7L);

        String finalValue = redisTemplate.opsForValue().get(stockKey);
        assertThat(finalValue).isEqualTo("7");

        log.info("✅ Redis DECR 테스트 성공: 10 → 9 → 8 → 7");

        // Clean up
        redisTemplate.delete(stockKey);
    }

    @Test
    @DisplayName("Redis DECR 음수 테스트")
    void redis_DECR_negative() {
        // Given
        String stockKey = "test:stock:negative";
        redisTemplate.opsForValue().set(stockKey, "1");

        // When
        Long remaining1 = redisTemplate.opsForValue().decrement(stockKey);  // 1 → 0
        Long remaining2 = redisTemplate.opsForValue().decrement(stockKey);  // 0 → -1
        Long remaining3 = redisTemplate.opsForValue().decrement(stockKey);  // -1 → -2

        // Then
        assertThat(remaining1).isEqualTo(0L);
        assertThat(remaining2).isEqualTo(-1L);
        assertThat(remaining3).isEqualTo(-2L);

        log.info("✅ Redis DECR 음수 테스트 성공: 1 → 0 → -1 → -2");

        // Clean up
        redisTemplate.delete(stockKey);
    }

    @Test
    @DisplayName("Redis 재고 초기화 테스트")
    void initializeRedisStock() {
        // When
        couponInventoryService.initializeAllRedisStock();

        // Then
        log.info("✅ Redis 재고 초기화 완료");
    }

    @Test
    @DisplayName("Redis-DB 동기화 성능 테스트")
    void performanceTest() {
        // Test 1: Redis DECR
        long redisStart = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            redisTemplate.opsForValue().decrement("test:perf");
        }
        long redisTime = (System.nanoTime() - redisStart) / 1_000_000; // ms

        // Then
        log.info("=".repeat(60));
        log.info("성능 테스트 결과:");
        log.info("  Redis DECR 100회: {} ms", redisTime);
        log.info("  평균: {} ms/op", String.format("%.2f", redisTime / 100.0));
        log.info("=".repeat(60));

        // Clean up
        redisTemplate.delete("test:perf");

        assertThat(redisTime).isLessThan(100); // 100ms 이내
    }
}
