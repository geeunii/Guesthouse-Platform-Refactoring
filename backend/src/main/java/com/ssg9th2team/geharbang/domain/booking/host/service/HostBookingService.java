package com.ssg9th2team.geharbang.domain.booking.host.service;

import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingResponse;
import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingListResponse;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface HostBookingService {
    HostBookingListResponse listBookings(
            Long hostUserId,
            YearMonth month,
            String viewClass,
            LocalDate startDate,
            LocalDate endDate,
            boolean upcomingOnly,
            String sort,
            String rangeMode,
            Integer page,
            Integer size,
            String cursor
    );

    HostBookingResponse getBooking(Long hostUserId, Long reservationId);
}
