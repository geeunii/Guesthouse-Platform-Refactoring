package com.ssg9th2team.geharbang.domain.dashboard.host.entity;

import lombok.Getter;

@Getter
public class HostSummaryRow {
    private long confirmedRevenue;
    private int confirmedReservations;
    private double avgRating;
    private int reviewCount;
    private int operatingAccommodations;
    private int totalAccommodations;
    private int statsCount;
}
