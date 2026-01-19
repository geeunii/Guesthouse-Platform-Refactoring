package com.ssg9th2team.geharbang.domain.admin.controller;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminBookingDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminBookingSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminRefundRequest;
import com.ssg9th2team.geharbang.domain.admin.service.AdminBookingService;
import com.ssg9th2team.geharbang.domain.admin.support.AdminId;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService bookingService;

    @GetMapping
    public AdminPageResponse<AdminBookingSummary> getBookings(
            @AdminId Long adminId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bookingService.getBookings(status, from, to, sort, page, size);
    }

    @GetMapping("/{reservationId}")
    public AdminBookingDetail getBookingDetail(
            @AdminId Long adminId,
            @PathVariable Long reservationId
    ) {
        return bookingService.getBookingDetail(reservationId);
    }

    @PostMapping("/{reservationId}/refund")
    public void refundBooking(
            @AdminId Long adminId,
            @PathVariable Long reservationId,
            @RequestBody AdminRefundRequest request
    ) {
        bookingService.processRefund(adminId, reservationId, request.refundAmount(), request.reason());
    }
}
