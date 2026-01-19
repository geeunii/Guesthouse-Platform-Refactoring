package com.ssg9th2team.geharbang.domain.coupon.service;

import com.ssg9th2team.geharbang.domain.coupon.dto.UserCouponResponseDto;
import com.ssg9th2team.geharbang.domain.coupon.entity.Coupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.CouponIssueResult;
import com.ssg9th2team.geharbang.domain.coupon.entity.CouponTriggerType;
import com.ssg9th2team.geharbang.domain.coupon.entity.UserCoupon;

import java.util.List;
import java.util.Set;

public interface UserCouponService {

    // 쿠폰 발급 (수동)
    void issueCoupon(Long userId, Long couponId);

    // 사용 가능 쿠폰, 사용 만료 쿠폰, 사용 완료한 쿠폰 조회 -> status 값만 바꾸면 됨 ISSUED / USED / EXPIRED
    List<UserCouponResponseDto> getMyCouponsByStatus(Long userId, String status);

    // 쿠폰 사용
    void useCoupon(Long userId, Long userCouponId);

    // 쿠폰 복구 (예약 취소 시 호출)
    void restoreCoupon(Long userId, Long userCouponId);


    // 첫 예약 쿠폰 발급 후 사용자가 예약 취소 -> 첫 예약 쿠폰 회수
    void revokeFirstReservationCoupon (Long userId);


    // 리뷰 3회 달성 시 쿠폰 자동 발급 (ReviewService에서 호출)
    boolean issueReviewRewardCoupon(Long userId);

    // 첫 예약 완료 시 쿠폰 자동 발급 (PaymentService에서 호출)
    boolean issueFirstReservationCoupon(Long userId);

    // 자동 발급
    boolean issueByTrigger(Long userId, CouponTriggerType trigger);


    // 공통 발급 로직
    CouponIssueResult issueToUser(Long userId, Coupon coupon);

    // 만료된 쿠폰 상태 변경 (스케줄러에서 호출)
    int expireOverdueCoupons();

    // 사용자 보유 쿠폰 ID 목록 조회
    Set<Long> getMyCouponIds(Long userId);
    
    // Redis 발급 이력 초기화
    void initializeRedisIssuedCoupons();
    
    // 특정 쿠폰 Redis 발급 이력 동기화
    void syncRedisIssuedCoupon(Long couponId);
    
    /**
     * 일일 선착순 쿠폰의 발급 이력을 초기화한다.
     * 매일 자정에 실행되어 사용자가 다시 쿠폰을 발급받을 수 있도록 함.
     * @return 초기화된 쿠폰 개수
     */
    int resetDailyCouponIssuedTracking();


}
