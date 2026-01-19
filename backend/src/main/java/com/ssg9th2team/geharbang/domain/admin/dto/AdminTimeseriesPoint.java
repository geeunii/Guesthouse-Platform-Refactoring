package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDate;

public record AdminTimeseriesPoint(
        LocalDate date,
        Long value
){
}
