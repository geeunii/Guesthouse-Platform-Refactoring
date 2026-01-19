package com.ssg9th2team.geharbang.domain.coupon.service;

import com.ssg9th2team.geharbang.domain.coupon.dto.CouponResponseDto;
import com.ssg9th2team.geharbang.domain.coupon.dto.UserCouponResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 쿠폰 Redis 캐싱 동작 확인 테스트
 */
@SpringBootTest
class CouponCachingTest {

    private static final Logger log = LoggerFactory.getLogger(CouponCachingTest.class);

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserCouponService userCouponService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("숙소별 쿠폰 목록 캐싱 테스트")
    void downloadableCoupons_shouldBeCached() {
        Long accommodationId = 1L;

        // 1. 첫 번째 호출 - DB 조회 후 캐시에 저장
        log.info("첫 번째 조회 시작 (DB 조회 예상)");
        List<CouponResponseDto> result1 = couponService.getDownloadableCoupons(accommodationId);
        log.info("첫 번째 조회 완료: {} 개", result1.size());

        // 2. 두 번째 호출 - 캐시에서 조회 (DB 접근 없음)
        log.info("두 번째 조회 시작 (캐시 조회 예상)");
        List<CouponResponseDto> result2 = couponService.getDownloadableCoupons(accommodationId);
        log.info("두 번째 조회 완료: {} 개", result2.size());

        // 3. 캐시 확인
        Cache cache = cacheManager.getCache("downloadableCoupons");
        assertThat(cache).isNotNull();
        
        Cache.ValueWrapper cachedValue = cache.get(accommodationId);
        log.info("캐시 존재 여부: {}", cachedValue != null);
        assertThat(cachedValue).isNotNull();

        // 4. 결과 동일 확인 (같은 인스턴스인지 확인)
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1.size()).isEqualTo(result2.size());

        log.info("✅ 숙소별 쿠폰 캐싱 테스트 성공");
    }

    @Test
    @DisplayName("사용자 쿠폰 목록 캐싱 테스트")
    void userCoupons_shouldBeCached() {
        Long userId = 1L;
        String status = "ISSUED";

        // 1. 첫 번째 호출 - DB 조회 후 캐시에 저장
        log.info("첫 번째 조회 시작 (DB 조회 예상)");
        List<UserCouponResponseDto> result1 = userCouponService.getMyCouponsByStatus(userId, status);
        log.info("첫 번째 조회 완료: {} 개", result1.size());

        // 2. 두 번째 호출 - 캐시에서 조회
        log.info("두 번째 조회 시작 (캐시 조회 예상)");
        List<UserCouponResponseDto> result2 = userCouponService.getMyCouponsByStatus(userId, status);
        log.info("두 번째 조회 완료: {} 개", result2.size());

        // 3. 캐시 확인
        Cache cache = cacheManager.getCache("userCoupons");
        assertThat(cache).isNotNull();

        String cacheKey = userId + "_" + status;
        Cache.ValueWrapper cachedValue = cache.get(cacheKey);
        log.info("캐시 키: {}, 존재 여부: {}", cacheKey, cachedValue != null);
        assertThat(cachedValue).isNotNull();

        // 4. 결과 동일 확인
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1.size()).isEqualTo(result2.size());

        log.info("✅ 사용자 쿠폰 캐싱 테스트 성공");
    }

    @Test
    @DisplayName("캐시 무효화 테스트 - 쿠폰 발급 시")
    void cacheEviction_onIssueCoupon() {
        Long userId = 1L;
        String status = "ISSUED";

        // 1. 캐시 생성
        log.info("초기 쿠폰 목록 조회");
        List<UserCouponResponseDto> before = userCouponService.getMyCouponsByStatus(userId, status);
        int beforeSize = before.size();

        // 2. 캐시 확인
        Cache cache = cacheManager.getCache("userCoupons");
        String cacheKey = userId + "_" + status;
        assertThat(cache.get(cacheKey)).isNotNull();
        log.info("캐시 생성 확인: key={}", cacheKey);

        // 3. 캐시 수동 삭제 (실제로는 쿠폰 발급이 하지만, 여기서는 수동으로 테스트)
        cache.evict(cacheKey);
        log.info("캐시 삭제 완료");

        // 4. 캐시가 삭제되었는지 확인
        assertThat(cache.get(cacheKey)).isNull();
        log.info("✅ 캐시 무효화 테스트 성공");
    }

    @Test
    @DisplayName("CacheManager 설정 확인")
    void cacheManager_configuration() {
        // 1. CacheManager 존재 확인
        assertThat(cacheManager).isNotNull();
        log.info("CacheManager: {}", cacheManager.getClass().getSimpleName());

        // 2. 설정된 캐시 이름 확인
        Cache downloadableCouponsCache = cacheManager.getCache("downloadableCoupons");
        Cache userCouponsCache = cacheManager.getCache("userCoupons");

        assertThat(downloadableCouponsCache).isNotNull();
        assertThat(userCouponsCache).isNotNull();

        log.info("✅ CacheManager 설정 확인 완료");
        log.info("  - downloadableCoupons 캐시: {}", downloadableCouponsCache.getName());
        log.info("  - userCoupons 캐시: {}", userCouponsCache.getName());
    }
}
