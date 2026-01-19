package com.ssg9th2team.geharbang.domain.payment.dto;

import com.ssg9th2team.geharbang.domain.payment.entity.Payment;

import java.time.LocalDateTime;

public record PaymentResponseDto(
        Long paymentId,
        Long reservationId,
        String orderId,
        String paymentKey,
        Integer requestAmount,
        Integer approvedAmount,
        String paymentMethod,
        Integer paymentStatus,
        LocalDateTime approvedAt,
        LocalDateTime createdAt) {
    public static PaymentResponseDto from(Payment payment) {
        return new PaymentResponseDto(
                payment.getId(),
                payment.getReservationId(),
                payment.getOrderId(),
                payment.getPgPaymentKey(),
                payment.getRequestAmount(),
                payment.getApprovedAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getApprovedAt(),
                payment.getCreatedAt());
    }
}
