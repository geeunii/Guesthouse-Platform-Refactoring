package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostForecastDiagnostics {
    private int trainingDays;
    private int backtestDays;
    private double mae;
    private double rmse;
    private double mape;
}
