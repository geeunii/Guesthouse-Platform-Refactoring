package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminAccommodationSummary(
        Long accommodationsId,
        Long hostUserId,
        String name,
        String category,
        String city,
        String district,
        String approvalStatus,
        String rejectionReason,
        LocalDateTime createdAt,
        Double avgRating,
        Integer reviewCount,
        Integer roomCount,
        Integer minPrice,
        String hostPhone,
        Integer reservationCount,
        Double occupancyRate,
        Double cancellationRate,
        Long totalRevenue
) {
}
