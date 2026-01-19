package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostThemeReportRow {
    private Long themeId;
    private String themeName;
    private Integer reservationCount;
    private Long revenueSum;
    private Integer accommodationCount;
}
