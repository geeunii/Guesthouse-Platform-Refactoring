package com.ssg9th2team.geharbang.domain.booking.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HostBookingListResponse {
    private List<HostBookingResponse> items;
    private HostBookingListMeta meta;
}
