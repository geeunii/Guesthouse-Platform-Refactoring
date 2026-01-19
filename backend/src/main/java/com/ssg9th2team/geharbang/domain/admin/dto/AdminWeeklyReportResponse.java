package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminWeeklyReportResponse(
        LocalDate from,
        LocalDate to,
        int days,
        boolean statsReady,
        long reservationCount,
        long paymentSuccessCount,
        long cancelCount,
        long refundCount,
        long refundAmount,
        long newUsers,
        long newHosts,
        long newAccommodations,
        long pendingAccommodations,
        List<AdminTimeseriesPoint> revenueSeries
) {
}
