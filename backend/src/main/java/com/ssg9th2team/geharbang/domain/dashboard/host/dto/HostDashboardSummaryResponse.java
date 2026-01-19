package com.ssg9th2team.geharbang.domain.dashboard.host.dto;

import com.ssg9th2team.geharbang.domain.dashboard.host.entity.HostSummaryRow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HostDashboardSummaryResponse {
    private final long confirmedRevenue;         // 이번 달 확정 수익
    private final int confirmedReservations;     // 이번 달 확정 예약 수
    private final double avgRating;              // 평균 평점
    private final int reviewCount;               // 리뷰 수
    private final int operatingAccommodations;   // 운영 중 숙소 수
    private final int totalAccommodations;       // 전체 숙소 수

    public static HostDashboardSummaryResponse from(HostSummaryRow row) {
        if (row == null) {
            return HostDashboardSummaryResponse.builder()
                    .confirmedRevenue(0L)
                    .confirmedReservations(0)
                    .avgRating(0.0)
                    .reviewCount(0)
                    .operatingAccommodations(0)
                    .totalAccommodations(0)
                    .build();
        }
        return HostDashboardSummaryResponse.builder()
                .confirmedRevenue(row.getConfirmedRevenue())
                .confirmedReservations(row.getConfirmedReservations())
                .avgRating(row.getAvgRating())
                .reviewCount(row.getReviewCount())
                .operatingAccommodations(row.getOperatingAccommodations())
                .totalAccommodations(row.getTotalAccommodations())
                .build();
    }
}
