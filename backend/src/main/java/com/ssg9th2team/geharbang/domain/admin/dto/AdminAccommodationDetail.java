package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminAccommodationDetail(
        Long accommodationsId,
        Long hostUserId,
        String hostName,
        String hostPhone,
        String name,
        String category,
        String city,
        String district,
        String township,
        String addressDetail,
        String approvalStatus,
        Integer accommodationStatus,
        String rejectionReason,
        LocalDateTime createdAt,
        Integer minPrice,
        Integer roomCount,
        Integer maxGuests,
        Double avgRating,
        Integer reviewCount,
        Integer reservationCount,
        Double occupancyRate,
        Double cancellationRate,
        Long totalRevenue
) {
}
