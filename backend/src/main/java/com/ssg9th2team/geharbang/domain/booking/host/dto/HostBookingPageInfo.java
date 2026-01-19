package com.ssg9th2team.geharbang.domain.booking.host.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostBookingPageInfo {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
