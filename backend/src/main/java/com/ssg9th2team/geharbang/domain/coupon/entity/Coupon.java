package com.ssg9th2team.geharbang.domain.coupon.entity;


import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 쿠폰 마스터 엔티티
 * - 쿠폰의 종류와 할인 정책을 정의하는 테이블
 * - 관리자가 생성하며, 유저에게 발급되면 UserCoupon이 생성됨
 *
 * 예시:
 *   - "리뷰 3회 작성 감사 쿠폰" (10% 할인)
 *   - "회원가입 축하 쿠폰" (5,000원 할인)
 *   - "여름 특가 이벤트 쿠폰" (15% 할인)
 */
@Entity
@Table(name = "coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Coupon {

    /**
     * 쿠폰 고유 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    /**
     * 쿠폰 식별 코드 (UNIQUE)
     * 예: "WELCOME", "REVIEW_3_REWARD", "SUMMER_2025"
     */
    @Column(name = "code")
    private String code;

    /**
     * 쿠폰 이름 (사용자에게 보여지는 이름)
     * 예: "회원가입 축하 쿠폰", "리뷰 감사 쿠폰"
     */
    @Column(name = "name")
    private String name;

    /**
     * 쿠폰 설명
     * 예: "첫 예약에 사용하세요!"
     */
    @Column(name = "description")
    private String description;

    /**
     * 할인 타입
     * - "FIXED": 정액 할인 (예: 5,000원 할인)
     * - "PERCENT": 정률 할인 (예: 10% 할인)
     */
    @Column(name = "discount_type")
    private String discountType;

    /**
     * 할인 값
     * - discountType이 "FIXED"면: 할인 금액 (예: 5000 = 5,000원)
     * - discountType이 "PERCENT"면: 할인율 (예: 10 = 10%)
     */
    @Column(name = "discount_value")
    private Integer discountValue;

    /**
     * 최소 주문 금액 (이 금액 이상일 때만 쿠폰 사용 가능)
     * 예: 50000 = 5만원 이상 결제 시 사용 가능
     */
    @Column(name = "min_price")
    private Integer minPrice;

    /**
     * 최대 할인 금액 (정률 할인 시 할인 상한선)
     * 예: 10% 할인이지만 최대 10,000원까지만 할인
     */
    @Column(name = "max_discount")
    private Integer maxDiscount;

    /**
     * 쿠폰 유효 시작일 (FIXED_PERIOD 타입에서 사용)
     * 예: 2025-07-01 00:00:00
     */
    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    /**
     * 쿠폰 유효 종료일 (FIXED_PERIOD 타입에서 사용)
     * 예: 2025-08-31 23:59:59
     */
    @Column(name = "valid_to")
    private LocalDateTime validTo;

    /**
     * 쿠폰 활성화 여부
     * - true: 발급 가능
     * - false: 발급 중지
     */
    @Column(name = "is_active", columnDefinition = "TINYINT")
    private Boolean isActive;

    /**
     * 쿠폰 생성일시
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 쿠폰 발급 트리거 타입 (어떤 조건에서 발급되는지)
     * - SIGNUP: 회원가입 시 자동 발급
     * - REVIEW_3: 리뷰 3회 달성 시 자동 발급
     * - DOWNLOAD: 사용자가 직접 다운로드
     * - EVENT: 관리자가 수동 지급
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type")
    private CouponTriggerType triggerType;

    /**
     * 유효기간 계산 방식
     * - DAYS_FROM_ISSUE: 발급일 기준 N일 후 만료 (예: 발급일로부터 30일)
     * - FIXED_PERIOD: 고정 기간 (validFrom ~ validTo 사용) 이벤트성 쿠폰
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "validity_type")
    private ValidityType validityType;

    /**
     * 발급일 기준 유효일수 (DAYS_FROM_ISSUE 타입에서 사용)
     * 예: 30 = 발급일로부터 30일 후 만료
     */
    @Column(name = "validity_days")
    private Integer validityDays;

    /**
     * 숙소 (숙소별 이벤트 쿠폰일 경우)
     * - null이면 전체 숙소에서 사용 가능
     * - 값이 있으면 해당 숙소에서만 사용 가능
     */
    @ManyToOne(fetch = FetchType.LAZY) // // 쿠폰(N) : 숙소(1) 관계
    @JoinColumn(name = "accommodations_id")
    private Accommodation accommodation;

    /**
     * 만료일 계산 메서드
     * - DAYS_FROM_ISSUE: 현재 시점 + validityDays
     * - FIXED_PERIOD: validTo 그대로 사용
     *
     * @return 계산된 만료일시
     */
    public LocalDateTime calculateExpiryDate() {
        if (this.validityType == ValidityType.DAYS_FROM_ISSUE && this.validityDays != null) {
            return LocalDateTime.now().plusDays(this.validityDays);
        }
        return this.validTo;
    }

}
