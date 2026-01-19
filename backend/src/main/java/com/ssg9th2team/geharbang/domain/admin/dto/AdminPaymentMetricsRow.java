package com.ssg9th2team.geharbang.domain.admin.dto;

public record AdminPaymentMetricsRow(
        Integer period,
        Long totalAmount,
        Long totalCount
) {
}
