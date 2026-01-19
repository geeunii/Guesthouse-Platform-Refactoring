package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostReviewReportTrendRow {
    private String period;
    private Integer reviewCount;
    private Double avgRating;
}
