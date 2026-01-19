package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminReportSummary(
        Long reportId,
        String type,
        String status,
        String title,
        LocalDateTime createdAt
) {
}
