package com.ssg9th2team.geharbang.domain.payment.dto;

import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;

import java.time.LocalDateTime;

public record RefundResponseDto(
        Long refundId,
        Long paymentId,
        Integer refundAmount,
        Integer refundStatus,
        String reasonCode,
        String reasonMessage,
        String requestedBy,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime createdAt) {
    public static RefundResponseDto from(PaymentRefund refund) {
        return new RefundResponseDto(
                refund.getId(),
                refund.getPaymentId(),
                refund.getRefundAmount(),
                refund.getRefundStatus(),
                refund.getReasonCode(),
                refund.getReasonMessage(),
                refund.getRequestedBy(),
                refund.getRequestedAt(),
                refund.getApprovedAt(),
                refund.getCreatedAt());
    }
}
