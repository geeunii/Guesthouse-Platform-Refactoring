package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HostDemandDailyRow {
    private LocalDate statDate;
    private Long reservationCount;
    private Long revenue;
}
