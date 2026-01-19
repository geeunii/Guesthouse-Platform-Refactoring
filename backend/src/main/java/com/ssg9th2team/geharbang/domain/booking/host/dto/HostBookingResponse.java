package com.ssg9th2team.geharbang.domain.booking.host.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class HostBookingResponse {
    private Long reservationId;
    private String reserverName;
    private String reserverPhone;
    private String accommodationName;
    private LocalDateTime checkin;
    private LocalDateTime checkout;
    private Integer guestCount;
    private Integer stayNights;
    private Integer finalPaymentAmount;
    private Integer reservationStatus;
    private Integer paymentStatus;
    private Integer refundStatus;
    private LocalDateTime createdAt;
}
