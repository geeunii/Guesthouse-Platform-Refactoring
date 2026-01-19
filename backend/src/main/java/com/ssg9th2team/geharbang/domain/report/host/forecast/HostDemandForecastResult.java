package com.ssg9th2team.geharbang.domain.report.host.forecast;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastBaseline;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastDaily;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastDiagnostics;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastSummary;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HostDemandForecastResult {
    private String modelVersion;
    private String explain;
    private HostForecastDiagnostics diagnostics;
    private HostForecastBaseline baseline;
    private HostForecastSummary summary;
    private List<HostForecastDaily> daily;
}
