package com.ssg9th2team.geharbang.domain.coupon.scheduler;

import com.ssg9th2team.geharbang.domain.coupon.service.CouponInventoryService;
import com.ssg9th2team.geharbang.domain.coupon.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponScheduler {

    private final UserCouponService userCouponService;
    private final CouponInventoryService couponInventoryService;

    /**
     * 애플리케이션 시작 시 Redis 재고 초기화
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("애플리케이션 시작 - Redis 쿠폰 데이터 초기화 시작");
        
        // 0. 서버 재시작 시 하루 지난 일일 쿠폰 자동 정리 (로컬 개발 환경 대응)
        log.info("서버 시작: 일일 쿠폰 초기화 체크 시작");
        userCouponService.resetDailyCouponIssuedTracking();
        
        // 1. 재고 초기화
        couponInventoryService.initializeAllRedisStock();
        
        // 2. 발급 이력 초기화 (일일 쿠폰 정리 후 실행)
        userCouponService.initializeRedisIssuedCoupons();
        
        log.info("Redis 쿠폰 데이터 초기화 완료");
    }

    /**
     * 매일 자정에 만료된 쿠폰 상태 변경 및 선착순 재고 초기화
     * 
     * ⚠️ 운영용: 매일 자정 실행
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void expireCoupons() {
        // 1. 만료된 쿠폰 상태 변경 (ISSUED → EXPIRED)
        int expired = userCouponService.expireOverdueCoupons();

        // 2. 선착순 재고 초기화 (50개 → 50개로 리셋)
        int reset = couponInventoryService.resetDailyInventories();

        // 3. 일일 쿠폰 발급 이력 초기화 (사용자가 다시 받을 수 있도록)
        int cleared = userCouponService.resetDailyCouponIssuedTracking();
        
        log.info("자정 배치 실행 완료 - 만료: {}, 재고 리셋: {}, 발급 이력 클리어: {}", 
                 expired, reset, cleared);
    }
}
