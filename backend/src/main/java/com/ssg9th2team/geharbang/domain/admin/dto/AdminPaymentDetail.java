package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminPaymentDetail(
        Long paymentId,
        Long reservationId,
        Long accommodationsId,
        Long userId,
        Integer reservationStatus,
        String orderId,
        String pgPaymentKey,
        Integer requestAmount,
        Integer approvedAmount,
        Integer paymentStatus,
        LocalDateTime approvedAt,
        Integer refundStatus,
        Integer refundAmount,
        LocalDateTime refundApprovedAt,
        String refundReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
