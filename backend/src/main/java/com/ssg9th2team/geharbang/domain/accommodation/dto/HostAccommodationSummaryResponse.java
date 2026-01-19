package com.ssg9th2team.geharbang.domain.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HostAccommodationSummaryResponse {
    private final Long accommodationsId;
    private final String accommodationsName;
    private final String status;
    private final String approvalStatus;
    private final Integer accommodationStatus;
    private final String rejectionReason;
    private final String city;
    private final String district;
    private final String township;
    private final String addressDetail;
    private final Integer maxGuests;
    private final Integer roomCount;
    private final Integer pricePerNight;
    private final String imageUrl;
}
