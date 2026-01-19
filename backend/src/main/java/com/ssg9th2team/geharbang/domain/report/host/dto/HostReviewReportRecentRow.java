package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class HostReviewReportRecentRow {
    private Long reviewId;
    private Long accommodationId;
    private String accommodationName;
    private Double rating;
    private String content;
    private String authorName;
    private String visitDate;
    private LocalDateTime createdAt;
    private Boolean isCrawled;
}
