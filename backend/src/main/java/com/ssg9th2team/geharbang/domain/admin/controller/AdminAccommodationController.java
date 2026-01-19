package com.ssg9th2team.geharbang.domain.admin.controller;

import com.ssg9th2team.geharbang.domain.accommodation.service.AccommodationGeoService;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationDetailResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminRejectRequest;
import com.ssg9th2team.geharbang.domain.admin.dto.GeoBackfillResponse;
import com.ssg9th2team.geharbang.domain.admin.service.AdminAccommodationService;
import com.ssg9th2team.geharbang.domain.admin.support.AdminId;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accommodations")
@RequiredArgsConstructor
public class AdminAccommodationController {

    private final AdminAccommodationService accommodationService;
    private final AccommodationGeoService accommodationGeoService;

    @GetMapping
    public AdminPageResponse<AdminAccommodationSummary> getAccommodations(
            @AdminId Long adminId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return accommodationService.getAccommodations(
                normalizeFilter(status),
                normalizeFilter(keyword),
                page,
                size,
                sort
        );
    }

    @GetMapping("/{accommodationId}")
    public AdminAccommodationDetailResponse getAccommodationDetail(
            @AdminId Long adminId,
            @PathVariable Long accommodationId
    ) {
        return accommodationService.getAccommodationDetail(accommodationId);
    }

    @PostMapping("/{accommodationId}/approve")
    public AdminAccommodationDetailResponse approveAccommodation(
            @AdminId Long adminId,
            @PathVariable Long accommodationId
    ) {
        return accommodationService.approveAccommodation(adminId, accommodationId);
    }

    @PostMapping("/{accommodationId}/reject")
    public AdminAccommodationDetailResponse rejectAccommodation(
            @AdminId Long adminId,
            @PathVariable Long accommodationId,
            @RequestBody AdminRejectRequest request
    ) {
        return accommodationService.rejectAccommodation(
                adminId,
                accommodationId,
                request != null ? request.reason() : null
        );
    }

    @PostMapping("/geo/backfill")
    public GeoBackfillResponse backfillMissingCoordinates(
            @AdminId Long adminId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return accommodationGeoService.backfillMissingCoordinates(limit);
    }

    private String normalizeFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if ("all".equalsIgnoreCase(normalized)
                || "undefined".equalsIgnoreCase(normalized)
                || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }
}
