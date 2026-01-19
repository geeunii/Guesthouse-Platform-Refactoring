package com.ssg9th2team.geharbang.domain.revenue.host.controller;

import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueDetailResponse;
import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueSummaryResponse;
import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueTrendResponse;
import com.ssg9th2team.geharbang.domain.revenue.host.service.HostRevenueService;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/host/revenue")
@RequiredArgsConstructor
public class HostRevenueController {

    private final HostRevenueService hostRevenueService;
    private final UserRepository userRepository;

    @GetMapping("/summary")
    public HostRevenueSummaryResponse summary(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication
    ) {
        String email = authentication.getName();
        Long hostId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
        return hostRevenueService.getSummary(hostId, year, month);
    }

    @GetMapping("/trend")
    public List<HostRevenueTrendResponse> trend(
            @RequestParam int year,
            Authentication authentication
    ) {
        String email = authentication.getName();
        Long hostId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
        return hostRevenueService.getTrend(hostId, year);
    }

    @GetMapping("/details")
    public List<HostRevenueDetailResponse> details(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String granularity,
            Authentication authentication
    ) {
        String email = authentication.getName();
        Long hostId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
        return hostRevenueService.getDetails(hostId, from, to, granularity);
    }
}
