package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class HostReviewAiSummaryResponse {
    private Long accommodationId;
    private LocalDate from;
    private LocalDate to;
    private String generatedAt;
    private List<String> overview;
    private List<String> positives;
    private List<String> negatives;
    private List<String> actions;
    private List<String> risks;
}
