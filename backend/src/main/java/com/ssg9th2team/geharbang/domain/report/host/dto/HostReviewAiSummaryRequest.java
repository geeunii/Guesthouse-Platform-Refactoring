package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HostReviewAiSummaryRequest {
    private Long accommodationId;
    private LocalDate from;
    private LocalDate to;
}
