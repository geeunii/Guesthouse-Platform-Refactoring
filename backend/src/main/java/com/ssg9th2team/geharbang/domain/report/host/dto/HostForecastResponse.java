package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class HostForecastResponse {
    private String target;
    private int horizonDays;
    private int historyDays;
    private LocalDate from;
    private LocalDate to;
    private Long accommodationId;
    private String modelVersion;
    private String explain;
    private HostForecastDiagnostics diagnostics;
    private HostForecastBaseline baseline;
    private List<HostForecastDaily> forecastDaily;
    private HostForecastSummary forecastSummary;
    private Integer historyPointCount;
}
