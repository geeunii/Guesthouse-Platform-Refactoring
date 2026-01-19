package com.ssg9th2team.geharbang.domain.coupon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 유저 쿠폰 엔티티
 * - 유저가 보유한 쿠폰 정보를 저장하는 테이블
 * - Coupon(마스터)과 User를 연결하는 중간 테이블 역할
 *
 * 관계:
 *   Coupon (1) ←────── (N) UserCoupon ──────→ (1) User
 *     │                      │
 *   "10% 할인쿠폰"         "유저A - 발급됨"
 *                          "유저B - 사용완료"
 *                          "유저C - 만료됨"
 */
@Entity
@Table(name = "user_coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserCoupon {

    /**
     * 유저 쿠폰 고유 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 쿠폰 마스터 ID (FK)
     * - 어떤 종류의 쿠폰인지 참조
     */
    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    /**
     * 유저 ID (FK)
     * - 쿠폰을 보유한 유저
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 쿠폰 발급일시
     * - 유저에게 쿠폰이 발급된 시점
     */
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    /**
     * 쿠폰 사용일시
     * - 유저가 쿠폰을 사용한 시점
     * - 미사용 시 null
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * 쿠폰 만료일시
     * - 이 시점 이후에는 쿠폰 사용 불가
     * - Coupon.calculateExpiryDate()로 계산되어 저장됨
     */
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    /**
     * 쿠폰 상태
     * - ISSUED: 사용 가능 (발급됨)
     * - USED: 사용 완료
     * - EXPIRED: 기간 만료
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserCouponStatus status = UserCouponStatus.ISSUED;


    /**
     * 쿠폰 사용 처리
     * - 상태를 USED로 변경하고 사용일시 기록
     */
    public void use() {
        this.status = UserCouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 만료 처리
     * - 상태를 EXPIRED로 변경 (배치 작업에서 호출)
     */
    public void expire() {
        this.status = UserCouponStatus.EXPIRED;
    }

    /**
     * 쿠폰 복구 처리 (예약 취소 시)
     * - 상태를 ISSUED로 변경하고 사용일시 초기화
     * - 만료일이 지나지 않은 경우에만 복구 가능
     */
    public void restore() {
        this.status = UserCouponStatus.ISSUED;
        this.usedAt = null;
    }

    /**
     * 쿠폰이 복구 가능한지 확인
     * - 만료일이 지나지 않아야 복구 가능
     */
    public boolean isRestorable() {
        return this.expiredAt == null || this.expiredAt.isAfter(LocalDateTime.now());
    }


    /**
     * 정적 팩토리 메서드 - 쿠폰 발급
     *
     * @param userId    쿠폰을 받을 유저 ID
     * @param couponId  발급할 쿠폰 마스터 ID
     * @param expiredAt 쿠폰 만료일시
     * @return 새로 생성된 UserCoupon 객체
     */
    public static UserCoupon issue(Long userId, Long couponId, LocalDateTime expiredAt) {
        return UserCoupon.builder()
                .userId(userId)
                .couponId(couponId)
                .issuedAt(LocalDateTime.now())
                .expiredAt(expiredAt)
                .build();  // status는 @Builder.Default로 ISSUED 자동 적용
    }

}
