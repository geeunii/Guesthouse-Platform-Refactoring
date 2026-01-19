package com.ssg9th2team.geharbang.domain.admin.controller;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminDashboardSummaryResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminIssueCenterResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminTimeseriesResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminWeeklyReportResponse;
import com.ssg9th2team.geharbang.domain.admin.service.AdminDashboardService;
import com.ssg9th2team.geharbang.domain.admin.support.AdminId;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/summary")
    public AdminDashboardSummaryResponse getDashboardSummary(
            @AdminId Long adminId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return dashboardService.getDashboardSummary(from, to);
    }

    @GetMapping("/timeseries")
    public AdminTimeseriesResponse getDashboardTimeseries(
            @AdminId Long adminId,
            @RequestParam String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return dashboardService.getTimeseries(metric, from, to);
    }

    @GetMapping("/issues")
    public AdminIssueCenterResponse getIssueCenter(
            @AdminId Long adminId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return dashboardService.getIssueCenter(from, to);
    }

    @GetMapping("/weekly")
    public AdminWeeklyReportResponse getWeeklyReport(
            @AdminId Long adminId,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return dashboardService.getWeeklyReport(days, from, to);
    }
}
