package com.ssg9th2team.geharbang.domain.reservation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    @Column(name = "accommodations_id", nullable = false)
    private Long accommodationsId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "checkin", nullable = false)
    private LocalDateTime checkin;

    @Column(name = "checkout", nullable = false)
    private LocalDateTime checkout;

    @Column(name = "stay_nights", nullable = false)
    private Integer stayNights;

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    @Column(name = "reservation_status", nullable = false)
    private Integer reservationStatus;

    @Column(name = "total_amount_before_dc", nullable = false)
    private Integer totalAmountBeforeDc;

    @Column(name = "user_coupon_id")
    private Long userCouponId;

    @Column(name = "coupon_discount_amount", nullable = false)
    private Integer couponDiscountAmount;

    @Column(name = "final_payment_amount", nullable = false)
    private Integer finalPaymentAmount;

    @Column(name = "payment_status", nullable = false)
    private Integer paymentStatus;

    @Column(name = "reserver_name", nullable = false, length = 50)
    private String reserverName;

    @Column(name = "reserver_phone", nullable = false, length = 20)
    private String reserverPhone;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false; // Soft Delete 여부

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (reservationStatus != null && reservationStatus == 3 && (paymentStatus == null || paymentStatus != 1)) {
            throw new IllegalStateException("체크인 완료는 결제 완료 상태에서만 가능합니다.");
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (reservationStatus != null && reservationStatus == 3 && (paymentStatus == null || paymentStatus != 1)) {
            throw new IllegalStateException("체크인 완료는 결제 완료 상태에서만 가능합니다.");
        }
    }

    /**
     * 결제 완료 시 상태 업데이트
     * - 예약상태: 2 (확정)
     * - 결제상태: 1 (완료)
     */
    public void updatePaymentCompleted() {
        this.reservationStatus = 2; // 예약 확정
        this.paymentStatus = 1; // 결제 완료
    }

    /**
     * 결제 실패 시 상태 업데이트
     * - 결제상태: 2 (실패)
     */
    public void updatePaymentFailed() {
        this.paymentStatus = 2; // 결제 실패
    }

    /**
     * 환불 시 상태 업데이트
     * - 예약상태: 9 (취소)
     * - 결제상태: 3 (환불)
     */
    public void updateRefunded() {
        this.reservationStatus = 9; // 예약 취소
        this.paymentStatus = 3; // 환불
    }
}
