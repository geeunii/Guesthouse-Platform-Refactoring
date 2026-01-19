package com.ssg9th2team.geharbang.domain.admin.dto;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.auth.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminAccommodationDetailResponse(
        Info info,
        Host host,
        Content content,
        List<AdminRoomResponse> rooms,
        Stats stats
) {
    public record Info(
            Long id,
            String name,
            String type,
            String status, // PENDING, APPROVED, REJECTED
            Integer operationStatus, // 0: 중지, 1: 운영
            String address,
            String addressDetail,
            String contact, // 숙소 대표 번호
            String bizNumber,
            String checkIn,
            String checkOut,
            LocalDateTime createdAt,
            String rejectionReason
    ) {}

    public record Host(
            Long hostId,
            String name,
            String phone,
            String email
    ) {}

    public record Content(
            String shortDescription,
            String description,
            List<String> images,
            List<String> amenities,
            String transportInfo,
            String parkingInfo
    ) {}

    public record Stats(
            Double rating,
            Integer reviewCount,
            Integer minPrice
    ) {}

    public static AdminAccommodationDetailResponse of(
            Accommodation accommodation,
            User hostUser,
            List<String> images,
            List<String> amenities,
            List<AdminRoomResponse> rooms
    ) {
        Info info = new Info(
                accommodation.getAccommodationsId(),
                accommodation.getAccommodationsName(),
                accommodation.getAccommodationsCategory() != null ? accommodation.getAccommodationsCategory().name() : null,
                accommodation.getApprovalStatus() != null ? accommodation.getApprovalStatus().name() : null,
                accommodation.getAccommodationStatus(),
                String.format("%s %s %s", accommodation.getCity(), accommodation.getDistrict(), accommodation.getTownship()),
                accommodation.getAddressDetail(),
                accommodation.getPhone(),
                accommodation.getBusinessRegistrationNumber(),
                accommodation.getCheckInTime(),
                accommodation.getCheckOutTime(),
                accommodation.getCreatedAt(),
                accommodation.getRejectionReason()
        );

        Host host = new Host(
                hostUser.getId(),
                hostUser.getName(),
                hostUser.getPhone(),
                hostUser.getEmail()
        );

        Content content = new Content(
                accommodation.getShortDescription(),
                accommodation.getAccommodationsDescription(),
                images,
                amenities,
                accommodation.getTransportInfo(),
                accommodation.getParkingInfo()
        );

        Stats stats = new Stats(
                accommodation.getRating(),
                accommodation.getReviewCount(),
                accommodation.getMinPrice()
        );

        return new AdminAccommodationDetailResponse(info, host, content, rooms, stats);
    }
}
