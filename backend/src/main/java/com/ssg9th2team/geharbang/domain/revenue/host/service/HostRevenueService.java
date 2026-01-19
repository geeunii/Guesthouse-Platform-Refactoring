package com.ssg9th2team.geharbang.domain.revenue.host.service;

import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueDetailResponse;
import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueSummaryResponse;
import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueTrendResponse;

import java.time.LocalDate;
import java.util.List;

public interface HostRevenueService {

    HostRevenueSummaryResponse getSummary(Long hostId, int year, int month);

    List<HostRevenueTrendResponse> getTrend(Long hostId, int year);

    List<HostRevenueDetailResponse> getDetails(Long hostId, LocalDate from, LocalDate to, String granularity);
}
