package com.ssg9th2team.geharbang.domain.holiday.service;

import com.ssg9th2team.geharbang.domain.holiday.dto.HolidayItemResponse;

import java.util.List;

public interface HolidayService {
    List<HolidayItemResponse> getHolidays(int year, Integer month);
}
