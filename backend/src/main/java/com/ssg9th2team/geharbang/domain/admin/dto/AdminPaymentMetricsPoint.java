package com.ssg9th2team.geharbang.domain.admin.dto;

public record AdminPaymentMetricsPoint(
        String label,
        long totalAmount,
        long totalCount
) {
}
