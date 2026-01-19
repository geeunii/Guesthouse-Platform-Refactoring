package com.ssg9th2team.geharbang.domain.admin.dto;

public record AdminAccommodationMetrics(
        Long accommodationsId,
        Integer reservationCount,
        Integer canceledCount,
        Integer totalCount,
        Long totalRevenue
) {
}
