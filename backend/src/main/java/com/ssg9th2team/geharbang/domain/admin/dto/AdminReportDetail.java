package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminReportDetail(
        Long reportId,
        Long reviewId,
        Long userId,
        String type,
        String state,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
