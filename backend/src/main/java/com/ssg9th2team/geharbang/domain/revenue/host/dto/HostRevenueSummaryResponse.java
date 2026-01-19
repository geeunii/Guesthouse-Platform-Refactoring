package com.ssg9th2team.geharbang.domain.revenue.host.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HostRevenueSummaryResponse {
    private final int year;
    private final int month;
    private final long totalRevenue;
    private final long expectedNextMonthRevenue;
    private final double platformFeeRate;
    private final long platformFeeAmount;
    private final int reservationCount;
}
