package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminBookingDetail(
        Long reservationId,
        Long accommodationsId,
        String accommodationName,
        Long hostUserId,
        Long userId,
        String guestEmail,
        String guestPhone,
        LocalDateTime checkin,
        LocalDateTime checkout,
        Integer guestCount,
        Integer reservationStatus,
        Integer paymentStatus,
        Integer finalPaymentAmount,
        Long paymentId,
        String orderId,
        String pgPaymentKey,
        String paymentMethod,
        Integer approvedAmount,
        LocalDateTime approvedAt,
        Integer refundStatus,
        Integer refundAmount,
        LocalDateTime refundApprovedAt,
        String refundReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
