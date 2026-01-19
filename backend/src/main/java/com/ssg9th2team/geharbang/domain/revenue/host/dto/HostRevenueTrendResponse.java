package com.ssg9th2team.geharbang.domain.revenue.host.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HostRevenueTrendResponse {
    private final int month;
    private final long revenue;
    private final int reservationCount;
    private final double occupancyRate;
}
