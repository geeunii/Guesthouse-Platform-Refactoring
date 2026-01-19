package com.ssg9th2team.geharbang.domain.reservation.dto;

import java.time.Instant;

public record ReservationRequestDto(
        Long accommodationsId,
        Long roomId,
        Long userId,
        Instant checkin,
        Instant checkout,
        Integer guestCount,
        Integer totalAmount,
        Long userCouponId,
        Integer couponDiscountAmount,
        String reserverName,
        String reserverPhone) {
    /**
     * userId가 null인 경우 기본값(1L)을 반환
     */
    public Long getUserIdOrDefault() {
        return userId != null ? userId : 1L;
    }
}
