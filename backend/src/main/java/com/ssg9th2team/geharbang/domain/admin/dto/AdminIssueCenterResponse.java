package com.ssg9th2team.geharbang.domain.admin.dto;

import java.util.List;

public record AdminIssueCenterResponse(
        long pendingAccommodations,
        long openReports,
        long refundCount,
        long paymentFailureCount,
        List<AdminAccommodationSummary> pendingAccommodationsList,
        List<AdminReportSummary> openReportsList
) {
}
