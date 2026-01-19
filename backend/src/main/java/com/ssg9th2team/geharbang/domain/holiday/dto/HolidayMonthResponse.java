package com.ssg9th2team.geharbang.domain.holiday.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HolidayMonthResponse {
    private int year;
    private Integer month;
    private List<HolidayItemResponse> holidays;
}
