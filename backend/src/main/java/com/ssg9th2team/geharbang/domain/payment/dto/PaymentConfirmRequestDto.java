package com.ssg9th2team.geharbang.domain.payment.dto;

public record PaymentConfirmRequestDto(
        String paymentKey, // 토스에서 발급한 결제 키
        String orderId, // 우리가 생성한 주문번호
        Integer amount // 결제 금액
) {
}
