package com.ssg9th2team.geharbang.domain.revenue.host.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HostRevenueSummaryRow {
    private final long totalRevenue;
    private final int reservationCount;
}
