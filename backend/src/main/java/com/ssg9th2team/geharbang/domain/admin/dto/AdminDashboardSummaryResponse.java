package com.ssg9th2team.geharbang.domain.admin.dto;

import java.util.List;

public record AdminDashboardSummaryResponse(
        long pendingAccommodations,
        long openReports,
        double pendingLeadTimeAvgDays,
        long overdueOpenReports48h,
        long weeklyRefundRequestCount,
        long weeklyRefundCompletedCount,
        double weeklyPaymentFailureRate,
        long weeklyPendingOver7Days,
        long reservationCount,
        long paymentSuccessAmount,
        double platformFeeRate,
        long platformFeeAmount,
        long paymentFailureCount,
        long refundRequestCount,
        long refundCompletedCount,
        long refundCompletedAmount,
        long netRevenue,
        List<AdminAccommodationSummary> pendingAccommodationsList,
        List<AdminReportSummary> openReportsList
) {
}
