package com.ssg9th2team.geharbang.domain.holiday.controller;

import com.ssg9th2team.geharbang.domain.holiday.dto.HolidayMonthResponse;
import com.ssg9th2team.geharbang.domain.holiday.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping("/holidays")
    public HolidayMonthResponse getHolidays(
            @RequestParam int year,
            @RequestParam(required = false) Integer month
    ) {
        return HolidayMonthResponse.builder()
                .year(year)
                .month(month)
                .holidays(holidayService.getHolidays(year, month))
                .build();
    }
}
