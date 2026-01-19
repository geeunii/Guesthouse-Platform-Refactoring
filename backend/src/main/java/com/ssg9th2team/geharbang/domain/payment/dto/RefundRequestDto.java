package com.ssg9th2team.geharbang.domain.payment.dto;

public record RefundRequestDto(
        Long reservationId,
        String cancelReason,
        Integer refundAmount) {
}
