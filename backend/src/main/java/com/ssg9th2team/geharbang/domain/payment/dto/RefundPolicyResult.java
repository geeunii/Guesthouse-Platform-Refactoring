package com.ssg9th2team.geharbang.domain.payment.dto;

public record RefundPolicyResult(
        int refundRate,
        int refundAmount,
        String policyCode,
        long daysBefore
) {
}
