package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class HostForecastBaseline {
    private double recentAvg7;
    private double recentAvg28;
    private Map<String, Double> weekdayFactors;
    private double level;
    private double trendPerDay;
    private double holidayFactor;
}
