package com.ssg9th2team.geharbang.domain.report.host.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HostForecastDaily {
    private LocalDate date;
    private String dowLabel;
    @JsonProperty("isWeekend")
    private boolean isWeekend;
    @JsonProperty("isHoliday")
    private boolean isHoliday;
    private double predictedValue;
    private double low;
    private double high;
}
