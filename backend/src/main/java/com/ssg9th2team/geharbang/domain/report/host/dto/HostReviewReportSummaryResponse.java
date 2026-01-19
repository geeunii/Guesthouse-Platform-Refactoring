package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class HostReviewReportSummaryResponse {
    private LocalDate from;
    private LocalDate to;
    private Long accommodationId;
    private Integer reviewCount;
    private Double avgRating;
    private Map<Integer, Integer> ratingDistribution;
    private List<HostReviewReportTagRow> topTags;
    private List<HostReviewReportRecentRow> recentReviews;
}
