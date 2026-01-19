package com.ssg9th2team.geharbang.domain.admin.dto;

public record AdminRefundRequest(
        Integer amount,
        String reason,
        Boolean override
) {
    // AdminBookingController와의 호환성을 위해 추가 메서드 제공
    public Integer refundAmount() {
        return amount;
    }
}
