package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HostAiInsightRequest {
    private String tab;
    private Long accommodationId;
    private LocalDate from;
    private LocalDate to;
    private String metric;
    private String target;
    private Integer horizonDays;
    private Integer historyDays;
    private boolean forceRefresh;
}
