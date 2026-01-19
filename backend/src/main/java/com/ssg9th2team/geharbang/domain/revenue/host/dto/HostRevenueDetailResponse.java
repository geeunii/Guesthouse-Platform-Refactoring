package com.ssg9th2team.geharbang.domain.revenue.host.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HostRevenueDetailResponse {
    private final String period;
    private final long revenue;
    private final double occupancyRate;
}
