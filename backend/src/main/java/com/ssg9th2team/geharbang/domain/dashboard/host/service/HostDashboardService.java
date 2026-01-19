package com.ssg9th2team.geharbang.domain.dashboard.host.service;

import com.ssg9th2team.geharbang.domain.dashboard.host.dto.HostDashboardSummaryResponse;
import com.ssg9th2team.geharbang.domain.dashboard.host.dto.TodayScheduleItemResponse;

import java.time.LocalDate;
import java.util.List;

public interface HostDashboardService {
    HostDashboardSummaryResponse getSummary(Long hostId, int year, int month);

    HostDashboardSummaryResponse getSummaryByRange(Long hostId, LocalDate start, LocalDate end);

    List<TodayScheduleItemResponse> getTodaySchedule(Long hostId, LocalDate date);
}
