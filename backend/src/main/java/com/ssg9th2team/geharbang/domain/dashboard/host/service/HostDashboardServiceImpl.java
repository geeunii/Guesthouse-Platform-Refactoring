package com.ssg9th2team.geharbang.domain.dashboard.host.service;

import com.ssg9th2team.geharbang.domain.dashboard.host.dto.HostDashboardSummaryResponse;
import com.ssg9th2team.geharbang.domain.dashboard.host.dto.TodayScheduleItemResponse;
import com.ssg9th2team.geharbang.domain.dashboard.host.repository.HostDashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostDashboardServiceImpl implements HostDashboardService {

    private final HostDashboardMapper hostDashboardMapper;

    @Override
    public HostDashboardSummaryResponse getSummary(Long hostId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);

        return resolveSummary(hostId, start, end);
    }

    @Override
    public HostDashboardSummaryResponse getSummaryByRange(Long hostId, LocalDate start, LocalDate end) {
        return resolveSummary(hostId, start, end);
    }

    @Override
    public List<TodayScheduleItemResponse> getTodaySchedule(Long hostId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return hostDashboardMapper.selectTodaySchedule(hostId, start, end);
    }

    private HostDashboardSummaryResponse resolveSummary(Long hostId, LocalDate start, LocalDate end) {
        var statsRow = hostDashboardMapper.selectHostSummary(hostId, start, end);
        if (statsRow == null) {
            return HostDashboardSummaryResponse.from(
                    hostDashboardMapper.selectHostSummaryFallback(hostId, start, end)
            );
        }
        if (statsRow.getStatsCount() == 0) {
            return HostDashboardSummaryResponse.from(
                    hostDashboardMapper.selectHostSummaryFallback(hostId, start, end)
            );
        }
        return HostDashboardSummaryResponse.from(statsRow);
    }
}
