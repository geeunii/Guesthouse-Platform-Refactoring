package com.ssg9th2team.geharbang.domain.dashboard.host.controller;

import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.dashboard.host.dto.HostDashboardSummaryResponse;
import com.ssg9th2team.geharbang.domain.dashboard.host.dto.TodayScheduleItemResponse;
import com.ssg9th2team.geharbang.domain.dashboard.host.service.HostDashboardService;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/host")
@RequiredArgsConstructor
public class HostDashboardController {

    private final HostDashboardService hostDashboardService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard/summary")
    public HostDashboardSummaryResponse summary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String range,
            Authentication authentication
    ) {
        String email = authentication.getName();
        Long hostId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
        if (range != null && !range.isBlank()) {
            LocalDate today = LocalDate.now();
            RangeWindow window = RangeWindow.from(range.trim(), today);
            return hostDashboardService.getSummaryByRange(hostId, window.start(), window.end());
        }

        int resolvedYear = year == null ? LocalDate.now().getYear() : year;
        int resolvedMonth = month == null ? LocalDate.now().getMonthValue() : month;
        return hostDashboardService.getSummary(hostId, resolvedYear, resolvedMonth);
    }

    @GetMapping("/dashboard/today-schedule")
    public List<TodayScheduleItemResponse> todaySchedule(
            @RequestParam LocalDate date,
            Authentication authentication
    ) {
        String email = authentication.getName();
        Long hostId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
        return hostDashboardService.getTodaySchedule(hostId, date);
    }

    private record RangeWindow(LocalDate start, LocalDate end) {
        static RangeWindow from(String range, LocalDate today) {
            return switch (range) {
                case "today" -> new RangeWindow(today, today.plusDays(1));
                case "7days" -> new RangeWindow(today.minusDays(6), today.plusDays(1));
                case "30days" -> new RangeWindow(today.minusDays(29), today.plusDays(1));
                case "year" -> {
                    LocalDate start = LocalDate.of(today.getYear(), 1, 1);
                    yield new RangeWindow(start, start.plusYears(1));
                }
                case "month" -> {
                    LocalDate start = YearMonth.from(today).atDay(1);
                    yield new RangeWindow(start, start.plusMonths(1));
                }
                default -> {
                    LocalDate start = YearMonth.from(today).atDay(1);
                    yield new RangeWindow(start, start.plusMonths(1));
                }
            };
        }
    }
}
