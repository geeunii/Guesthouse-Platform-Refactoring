package com.ssg9th2team.geharbang.domain.admin.controller;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminReportDetailResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminReportResolveRequest;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminReportSummary;
import com.ssg9th2team.geharbang.domain.admin.service.AdminReportService;
import com.ssg9th2team.geharbang.domain.admin.support.AdminId;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService reportService;

    @GetMapping
    public AdminPageResponse<AdminReportSummary> getReports(
            @AdminId Long adminId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return reportService.getReports(
                normalizeFilter(status),
                normalizeFilter(type),
                normalizeFilter(keyword),
                page,
                size,
                sort
        );
    }

    @GetMapping("/{reportId}")
    public AdminReportDetailResponse getReportDetail(
            @AdminId Long adminId,
            @PathVariable Long reportId
    ) {
        return reportService.getReportDetail(reportId);
    }

    @PostMapping("/{reportId}/resolve")
    public AdminReportDetailResponse resolveReport(
            @AdminId Long adminId,
            @PathVariable Long reportId,
            @RequestBody AdminReportResolveRequest request
    ) {
        String action = request != null ? request.action() : null;
        String memo = request != null ? request.memo() : null;
        return reportService.resolveReport(adminId, reportId, action, memo);
    }

    private String normalizeFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if ("all".equalsIgnoreCase(normalized)
                || "undefined".equalsIgnoreCase(normalized)
                || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }
}
