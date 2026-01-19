package com.ssg9th2team.geharbang.domain.booking.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HostBookingListMeta {
    private String appliedSort;
    private String rangeMode;
    private LocalDate rangeFrom;
    private LocalDate rangeTo;
    private HostBookingPageInfo pageInfo;
    private HostBookingCursorInfo cursorInfo;
}
