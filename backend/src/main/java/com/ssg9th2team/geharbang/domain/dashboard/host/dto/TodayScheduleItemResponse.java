package com.ssg9th2team.geharbang.domain.dashboard.host.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@Builder
@AllArgsConstructor
public class TodayScheduleItemResponse {
    private final String type;            // CHECKIN / CHECKOUT
    private final LocalTime time;         // e.g. 15:00 / 11:00
    private final Long reservationId;
    private final String accommodationName;
    private final String roomName;
    private final String guestName;
    private final String requestNote;
    private final String phone;
}
