package com.ssg9th2team.geharbang.domain.booking.host.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostBookingCursorInfo {
    private boolean hasNext;
    private String nextCursor;
}
