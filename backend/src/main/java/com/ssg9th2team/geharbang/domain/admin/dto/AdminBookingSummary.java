package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminBookingSummary(
        Long reservationId,
        Long accommodationsId,
        Long userId,
        LocalDateTime checkin,
        LocalDateTime checkout,
        Integer guestCount,
        Integer reservationStatus,
        Integer paymentStatus,
        Integer finalPaymentAmount,
        LocalDateTime createdAt
) {
}
