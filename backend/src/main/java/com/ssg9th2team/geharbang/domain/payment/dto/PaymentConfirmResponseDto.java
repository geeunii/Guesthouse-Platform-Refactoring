package com.ssg9th2team.geharbang.domain.payment.dto;

public record PaymentConfirmResponseDto(
        PaymentResponseDto payment,
        boolean couponIssued,
        String couponName
) {
    public static PaymentConfirmResponseDto of(PaymentResponseDto payment, boolean couponIssued, String couponName) {
        return new PaymentConfirmResponseDto(payment, couponIssued, couponName);
    }

    public static PaymentConfirmResponseDto of(PaymentResponseDto payment, boolean couponIssued) {
        return new PaymentConfirmResponseDto(payment, couponIssued, couponIssued ? "첫 예약 감사 쿠폰" : null);
    }
}
