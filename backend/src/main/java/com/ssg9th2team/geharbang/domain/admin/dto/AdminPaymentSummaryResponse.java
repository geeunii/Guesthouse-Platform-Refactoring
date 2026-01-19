package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDate;

public record AdminPaymentSummaryResponse(
        LocalDate from,
        LocalDate to,
        long grossAmount,
        long refundCompletedAmount,
        long netAmount,
        long successCount,
        long failureCount,
        long refundRequestCount,
        long refundCompletedCount,
        double platformFeeRate,
        long platformFeeAmount
) {
}
