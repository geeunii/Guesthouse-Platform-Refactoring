package com.ssg9th2team.geharbang.domain.coupon.entity;

/**
 * 유저 쿠폰 상태
 */
public enum UserCouponStatus {
    ISSUED,   // 사용 가능 (발급됨)
    USED,     // 사용 완료
    EXPIRED   // 기간 만료
}
