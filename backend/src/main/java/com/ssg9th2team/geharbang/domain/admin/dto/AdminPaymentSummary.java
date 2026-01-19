package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminPaymentSummary(
        Long paymentId,
        Long reservationId,
        String orderId,
        String paymentKey,
        Integer approvedAmount,
        Integer paymentStatus,
        Integer reservationStatus,
        LocalDateTime checkin,
        LocalDateTime createdAt
) {
}
