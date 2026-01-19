package com.ssg9th2team.geharbang.domain.coupon.entity;

/**
 * 쿠폰 발급 트리거 타입
 * - 쿠폰이 어떤 조건에서 발급되는지 정의
 *
 * 사용 예시:
 *   - SIGNUP 쿠폰: 회원가입 시 AuthService에서 자동 발급
 *   - REVIEW_3 쿠폰: 리뷰 3회 작성 시 ReviewService에서 자동 발급
 *   - DOWNLOAD 쿠폰: 숙소 상세페이지에서 유저가 직접 다운로드
 */
public enum CouponTriggerType {

    /**
     * 회원가입 시 자동 발급
     * - AuthService.signup() 에서 호출
     * - 예: "회원가입 축하 5,000원 할인쿠폰"
     */
    SIGNUP,

    /**
     * 리뷰 3회 달성 시 자동 발급
     * - ReviewService.createReview() 에서 호출
     * - 리뷰 개수가 3의 배수(3, 6, 9...)일 때 발급
     * - 예: "리뷰 작성 감사 10% 할인쿠폰"
     */
    REVIEW_3,

    /**
     * 리뷰 10회 달성 시 자동 발급
     * - 충성 고객 보상용
     * - 예: "리뷰 마니아 20% 할인쿠폰"
     */
    REVIEW_10,

    /**
     * 첫 예약 완료 시 자동 발급
     * - ReservationService에서 첫 예약 확정 시 호출
     * - 예: "첫 예약 감사 쿠폰"
     */
    FIRST_RESERVATION,

    /**
     * 사용자가 직접 다운로드
     * - 숙소 상세페이지, 이벤트 페이지에서 "쿠폰 받기" 버튼 클릭
     * - CouponController.issueByDownload() 에서 처리
     * - 예: "여름 특가 이벤트 쿠폰", "숙소 전용 할인쿠폰"
     */
    DOWNLOAD,

    /**
     * 관리자가 수동 지급
     * - 관리자 페이지에서 특정 유저에게 직접 발급
     * - 고객 불만 처리, VIP 보상 등에 사용
     */
    EVENT

}
