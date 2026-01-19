package com.ssg9th2team.geharbang.domain.holiday.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HolidayItemResponse {
    private String date;
    private String name;

    @JsonProperty("isHoliday")
    private boolean isHoliday;
}
