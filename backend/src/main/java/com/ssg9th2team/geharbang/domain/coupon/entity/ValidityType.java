package com.ssg9th2team.geharbang.domain.coupon.entity;

/**
 * 쿠폰 유효기간 계산 방식
 * - 쿠폰의 만료일을 어떻게 계산할지 정의
 *
 * 사용 예시:
 *   - 회원가입 쿠폰: DAYS_FROM_ISSUE (발급일로부터 30일)
 *   - 리뷰 보상 쿠폰: DAYS_FROM_ISSUE (발급일로부터 90일)
 *   - 이벤트 쿠폰: FIXED_PERIOD (2025-07-01 ~ 2025-08-31)
 */
public enum ValidityType {

    /**
     * 발급일 기준 N일 후 만료
     * - Coupon.validityDays 값을 사용
     * - 유저마다 쿠폰 만료일이 다름 (발급 시점에 따라)
     *
     * 예시:
     *   - validityDays = 30 → 발급일로부터 30일 후 만료
     *   - 1월 1일 발급 → 1월 31일 만료
     *   - 2월 15일 발급 → 3월 17일 만료
     */
    DAYS_FROM_ISSUE,

    /**
     * 고정 기간 (특정 날짜 범위)
     * - Coupon.validFrom ~ Coupon.validTo 값을 사용
     * - 모든 유저가 동일한 만료일을 가짐
     *
     * 예시:
     *   - validFrom = 2025-07-01, validTo = 2025-08-31
     *   - 7월 1일부터 8월 31일까지만 사용 가능
     *   - 이벤트/시즌 쿠폰에 적합
     */
    FIXED_PERIOD

}
