package com.ssg9th2team.geharbang.domain.booking.host.controller;

import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingListResponse;
import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingResponse;
import com.ssg9th2team.geharbang.domain.booking.host.service.HostBookingService;
import com.ssg9th2team.geharbang.domain.booking.host.support.HostIdentityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/host/booking")
@RequiredArgsConstructor
public class HostBookingController {

    private final HostBookingService hostBookingService;
    private final HostIdentityResolver hostIdentityResolver;

    @GetMapping
    public HostBookingListResponse listBookings(
            @RequestParam(required = false) String month,
            @RequestParam(required = false, name = "class") String viewClass,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "false") boolean upcomingOnly,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String rangeMode,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String cursor,
            Authentication authentication
    ) {
        Long hostUserId = hostIdentityResolver.resolveHostUserId(authentication);
        YearMonth yearMonth = (month != null && !month.isBlank()) ? YearMonth.parse(month) : null;
        return hostBookingService.listBookings(
                hostUserId,
                yearMonth,
                viewClass,
                startDate,
                endDate,
                upcomingOnly,
                sort,
                rangeMode,
                page,
                size,
                cursor
        );
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<HostBookingResponse> getBooking(
            @PathVariable Long reservationId,
            Authentication authentication
    ) {
        Long hostUserId = hostIdentityResolver.resolveHostUserId(authentication);
        HostBookingResponse response = hostBookingService.getBooking(hostUserId, reservationId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
