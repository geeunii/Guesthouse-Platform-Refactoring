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
 * Redis Set 기반 중복 발급 체크 테스트
 */
@SpringBootTest
class CouponDuplicateCheckTest {

    private static final Logger log = LoggerFactory.getLogger(CouponDuplicateCheckTest.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Redis Set - 중복 체크 테스트")
    void redis_set_duplicate_check() {
        // Given
        String redisKey = "coupon:issued:test";
        String userId1 = "1";
        String userId2 = "2";
        String userId1Dup = "1"; // 중복

        // When
        Long result1 = redisTemplate.opsForSet().add(redisKey, userId1);
        Long result2 = redisTemplate.opsForSet().add(redisKey, userId2);
        Long result3 = redisTemplate.opsForSet().add(redisKey, userId1Dup);

        // Then
        assertThat(result1).isEqualTo(1L);   // 첫 발급 성공
        assertThat(result2).isEqualTo(1L);   // 다른 사용자 성공
        assertThat(result3).isEqualTo(0L);  // 중복 발급 차단!

        // Set 크기 확인
        Long size = redisTemplate.opsForSet().size(redisKey);
        assertThat(size).isEqualTo(2L);  // userId 1, 2만 저장

        log.info("✅ Redis Set 중복 체크 성공");

        // Clean up
        redisTemplate.delete(redisKey);
    }

    @Test
    @DisplayName("Redis Set - 멤버 존재 확인 (isMember)")
    void redis_set_is_member() {
        // Given
        String redisKey = "coupon:issued:test2";
        redisTemplate.opsForSet().add(redisKey, "1", "2", "3");

        // When
        Boolean exists1 = redisTemplate.opsForSet().isMember(redisKey, "1");
        Boolean exists2 = redisTemplate.opsForSet().isMember(redisKey, "999");

        // Then
        assertThat(exists1).isTrue();   // 존재함
        assertThat(exists2).isFalse();  // 존재 안 함

        log.info("✅ Redis Set isMember 테스트 성공");

        // Clean up
        redisTemplate.delete(redisKey);
    }

    @Test
    @DisplayName("Redis Set - 대량 추가 성능 테스트")
    void redis_set_performance() {
        // Given
        String redisKey = "coupon:issued:perf";
        int userCount = 1000;

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 1; i <= userCount; i++) {
            redisTemplate.opsForSet().add(redisKey, String.valueOf(i));
        }
        
        long duration = System.currentTimeMillis() - startTime;

        // Then
        Long size = redisTemplate.opsForSet().size(redisKey);
        assertThat(size).isEqualTo((long) userCount);

        log.info("=".repeat(60));
        log.info("Redis Set 성능 테스트:");
        log.info("  · 추가한 사용자 수: {}", userCount);
        log.info("  · 소요 시간: {} ms", duration);
        log.info("  · 평균: {} ms/op", String.format("%.3f", duration / (double) userCount));
        log.info("=".repeat(60));

        assertThat(duration).isLessThan(1000); // 1초 이내

        // Clean up
        redisTemplate.delete(redisKey);
    }

    @Test
    @DisplayName("Redis Set - 중복 추가 시 false 반환")
    void redis_set_returns_false_on_duplicate() {
        // Given
        String redisKey = "coupon:issued:dup";
        String userId = "123";

        // When
        Long first = redisTemplate.opsForSet().add(redisKey, userId);
        Long second = redisTemplate.opsForSet().add(redisKey, userId);
        Long third = redisTemplate.opsForSet().add(redisKey, userId);

        // Then
        assertThat(first).isEqualTo(1L);
        assertThat(second).isEqualTo(0L);
        assertThat(third).isEqualTo(0L);

        log.info("✅ 중복 추가 시 false 반환 확인");

        // Clean up
        redisTemplate.delete(redisKey);
    }
}
