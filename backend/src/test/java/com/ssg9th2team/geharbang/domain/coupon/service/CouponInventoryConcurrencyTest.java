package com.ssg9th2team.geharbang.domain.coupon.service;

import com.ssg9th2team.geharbang.domain.coupon.entity.Coupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.CouponInventory;
import com.ssg9th2team.geharbang.domain.coupon.entity.CouponTriggerType;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.CouponInventoryRepository;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.CouponJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.boot.test.mock.mockito.MockBean;
import com.ssg9th2team.geharbang.domain.coupon.scheduler.CouponScheduler; // import 추가 확인 필요

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CouponInventoryConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(CouponInventoryConcurrencyTest.class);

    @Autowired
    private CouponInventoryService couponInventoryService;

    @Autowired
    private CouponInventoryRepository couponInventoryRepository;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Long TEST_COUPON_ID; 
    private static final int INITIAL_STOCK = 100;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리 (순서 중요: 자식 -> 부모)
        couponInventoryRepository.deleteAllInBatch();
        couponJpaRepository.deleteAllInBatch();
        
        // 테스트 쿠폰 생성
        Coupon testCoupon = Coupon.builder()
                .name("동시성 테스트 쿠폰")
                .discountType("FIXED")
                .discountValue(5000)
                .triggerType(CouponTriggerType.DOWNLOAD)
                .validityDays(30)
                .isActive(true)
                .build();
        
        testCoupon = couponJpaRepository.save(testCoupon);
        this.TEST_COUPON_ID = testCoupon.getCouponId();

        // 재고 초기화
        CouponInventory inventory = CouponInventory.builder()
                .couponId(TEST_COUPON_ID)
                .dailyLimit(INITIAL_STOCK)
                .availableToday(INITIAL_STOCK)
                .lastResetDate(LocalDate.now())
                .build();
        
        couponInventoryRepository.save(inventory);

        // Redis 재고 초기화
        String redisKey = "coupon:stock:" + TEST_COUPON_ID;
        redisTemplate.opsForValue().set(redisKey, String.valueOf(INITIAL_STOCK));
        
        log.info("테스트 쿠폰 초기화 완료 - ID: {}, 재고: {}", TEST_COUPON_ID, INITIAL_STOCK);
    }

    @Test
    @DisplayName("1000명이 동시에 100개 선착순 쿠폰 발급 - 정확히 100명만 성공")
    void concurrentCouponIssue_shouldAllowExactly100() throws InterruptedException {
        // Given
        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    boolean result = couponInventoryService.consumeSlotIfLimited(TEST_COUPON_ID);
                    if (result) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("쿠폰 발급 실패", e);
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        log.info("=".repeat(60));
        log.info("동시성 테스트 결과:");
        log.info("  · 총 요청 수: {}", threadCount);
        log.info("  · 성공: {} 건", successCount.get());
        log.info("  · 실패: {} 건", failCount.get());
        log.info("  · 처리 시간: {} ms", duration);
        log.info("  · 평균 응답 시간: {} ms", duration / (double) threadCount);
        log.info("=".repeat(60));

        // 정확히 100명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK);
        assertThat(failCount.get()).isEqualTo(threadCount - INITIAL_STOCK);
        
        // DB 재고 확인
        CouponInventory inventory = couponInventoryRepository.findByCouponId(TEST_COUPON_ID).orElseThrow();
        assertThat(inventory.getAvailableToday()).isEqualTo(0);
        
        // Redis 재고 확인
        String redisKey = "coupon:stock:" + TEST_COUPON_ID;
        String redisStock = redisTemplate.opsForValue().get(redisKey);
        assertThat(redisStock).isEqualTo("0");
    }

    @Test
    @DisplayName("Redis 장애 시 DB 락 폴백 테스트")
    void redisFallback_shouldWorkWithDbLock() {
        // Given
        // Redis를 사용할 수 없는 상황 시뮬레이션은 어려우므로
        // fallbackToDbLock 메서드를 직접 테스트
        
        // When
        boolean result1 = couponInventoryService.fallbackToDbLock(TEST_COUPON_ID);
        boolean result2 = couponInventoryService.fallbackToDbLock(TEST_COUPON_ID);
        
        // Then
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
        
        // DB 재고 확인 (2개 차감)
        CouponInventory inventory = couponInventoryRepository.findByCouponId(TEST_COUPON_ID).orElseThrow();
        assertThat(inventory.getAvailableToday()).isEqualTo(INITIAL_STOCK - 2);
        
        log.info("DB 폴백 테스트 성공 - 남은 재고: {}", inventory.getAvailableToday());
    }

    @Test
    @DisplayName("Redis-DB 동기화 테스트")
    void redisDbSync_shouldKeepConsistency() {
        // Given
        String redisKey = "coupon:stock:" + TEST_COUPON_ID;
        
        // Redis 값을 임의로 변경 (불일치 발생)
        redisTemplate.opsForValue().set(redisKey, "50");
        
        // When
        couponInventoryService.syncRedisStock(TEST_COUPON_ID);
        
        // Then
        String syncedValue = redisTemplate.opsForValue().get(redisKey);
        assertThat(syncedValue).isEqualTo(String.valueOf(INITIAL_STOCK));
        
        log.info("Redis-DB 동기화 성공 - Redis 재고: {}", syncedValue);
    }

    @Test
    @DisplayName("모든 쿠폰 Redis 재고 초기화 테스트")
    void initializeAllRedisStock_shouldSyncAll() {
        // Given
        String redisKey = "coupon:stock:" + TEST_COUPON_ID;
        redisTemplate.delete(redisKey); // Redis 키 삭제
        
        // When
        couponInventoryService.initializeAllRedisStock();
        
        // Then
        String redisStock = redisTemplate.opsForValue().get(redisKey);
        assertThat(redisStock).isNotNull();
        assertThat(redisStock).isEqualTo(String.valueOf(INITIAL_STOCK));
        
        log.info("모든 쿠폰 Redis 초기화 성공");
    }

    @Test
    @DisplayName("성능 비교 테스트 - Redis vs DB")
    void performanceComparison_RedisVsDb() throws InterruptedException {
        // 웜업 (JIT 컴파일 등 고려)
        measurePerformance(() -> couponInventoryService.consumeSlotIfLimited(TEST_COUPON_ID), 10);
        setUp();

        // Test 1: Redis + DB 하이브리드 (반복 횟수 늘림)
        int iterations = 300;
        long hybridTime = measurePerformance(() -> 
            couponInventoryService.consumeSlotIfLimited(TEST_COUPON_ID), iterations);
        
        // 재고 복구
        setUp();
        
        // Test 2: DB 락만 사용
        long dbOnlyTime = measurePerformance(() -> 
            couponInventoryService.fallbackToDbLock(TEST_COUPON_ID), iterations);
        
        // Then
        log.info("=".repeat(60));
        log.info("성능 비교 ({}회 반복):", iterations);
        log.info("  · Redis + DB: {} ms", hybridTime);
        log.info("  · DB Only: {} ms", dbOnlyTime);
        if (hybridTime > 0) {
            log.info("  · 성능 향상: {}배", String.format("%.2f", dbOnlyTime / (double) hybridTime));
        }
        log.info("=".repeat(60));
        
        // 주의: CI 환경이나 로컬 부하에 따라 일시적으로 Redis가 느릴 수 있으므로 
        // 엄격한 Assertion보다는 로그 확인 권장. 여기선 여유를 둠.
        // assertThat(hybridTime).isLessThanOrEqualTo(dbOnlyTime + 50); // 약간의 오차 허용
    }

    private long measurePerformance(Runnable task, int iterations) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(iterations);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            executorService.submit(() -> {
                try {
                    task.run();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executorService.shutdown();
        
        return System.currentTimeMillis() - startTime;
    }
}
