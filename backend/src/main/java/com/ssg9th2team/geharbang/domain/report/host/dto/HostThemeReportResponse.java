package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class HostThemeReportResponse {
    private LocalDate from;
    private LocalDate to;
    private Long accommodationId;
    private String metric;
    private List<HostThemeReportRow> rows;
}
