package com.ssg9th2team.geharbang.domain.report.host.controller;

import com.ssg9th2team.geharbang.domain.booking.host.support.HostIdentityResolver;
import com.ssg9th2team.geharbang.domain.report.host.ai.HostAiInsightService;
import com.ssg9th2team.geharbang.domain.report.host.ai.HostAiInsightEligibilityChecker;
import com.ssg9th2team.geharbang.domain.report.host.ai.HostAiInsightEligibilityResult;
import com.ssg9th2team.geharbang.domain.report.host.ai.HostAiInsightTab;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostAiInsightEligibilityResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostAiInsightRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostAiInsightResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportTrendRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportResponse;
import com.ssg9th2team.geharbang.domain.report.host.service.HostReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/host/reports")
@RequiredArgsConstructor
public class HostReportController {

    private final HostReportService hostReportService;
    private final HostIdentityResolver hostIdentityResolver;
    private final HostAiInsightService hostAiInsightService;
    private final HostAiInsightEligibilityChecker eligibilityChecker;

    @GetMapping("/reviews/summary")
    public HostReviewReportSummaryResponse reviewSummary(
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        return hostReportService.getReviewSummary(hostId, accommodationId, from, to);
    }

    @GetMapping("/reviews/trend")
    public List<HostReviewReportTrendRow> reviewTrend(
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(defaultValue = "6") int months,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        return hostReportService.getReviewTrend(hostId, accommodationId, months);
    }

    @GetMapping("/themes/popular")
    public HostThemeReportResponse themePopularity(
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "reservations") String metric,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        return hostReportService.getThemePopularity(hostId, accommodationId, from, to, metric);
    }

    @GetMapping("/forecast/demand")
    public HostForecastResponse forecastDemand(
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(defaultValue = "reservations") String target,
            @RequestParam(defaultValue = "30") int horizonDays,
            @RequestParam(defaultValue = "180") int historyDays,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        return hostReportService.getDemandForecast(hostId, accommodationId, target, horizonDays, historyDays);
    }

    @PostMapping("/reviews/ai-summary")
    public HostReviewAiSummaryResponse reviewAiSummary(
            @RequestBody HostReviewAiSummaryRequest request,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        if (request == null) {
            request = new HostReviewAiSummaryRequest();
        }
        return hostReportService.getReviewAiSummary(
                hostId,
                request.getAccommodationId(),
                request.getFrom(),
                request.getTo()
        );
    }

    /**
     * Host AI insight endpoint for REVIEW/THEME/DEMAND tabs.
     * Chosen to keep existing /reviews/ai-summary stable while allowing tab expansion.
     */
    @PostMapping("/ai-insight")
    public HostAiInsightResponse aiInsight(
            @RequestBody HostAiInsightRequest request,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        if (request == null) {
            request = new HostAiInsightRequest();
        }
        return hostAiInsightService.generate(hostId, request);
    }

    @GetMapping("/ai-insight/eligibility")
    public HostAiInsightEligibilityResponse aiInsightEligibility(
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "reservations") String metric,
            @RequestParam(required = false, defaultValue = "180") Integer historyDays,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        HostAiInsightRequest request = new HostAiInsightRequest();
        request.setAccommodationId(accommodationId);
        request.setFrom(from);
        request.setTo(to);
        request.setMetric(metric);
        request.setHistoryDays(historyDays);

        HostAiInsightEligibilityResponse response = new HostAiInsightEligibilityResponse();
        boolean tabProvided = tab != null && !tab.isBlank();
        HostAiInsightTab target = parseTab(tab);
        if (tabProvided && target == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tab value: " + tab);
        }
        if (target != null) {
            HostAiInsightEligibilityResult result = eligibilityChecker.evaluate(target, request, hostId);
            applyEligibilityResponse(response, target, result);
            return response;
        }
        response.setReview(eligibilityChecker.evaluate(HostAiInsightTab.REVIEW, request, hostId));
        response.setTheme(eligibilityChecker.evaluate(HostAiInsightTab.THEME, request, hostId));
        response.setDemand(eligibilityChecker.evaluate(HostAiInsightTab.DEMAND, request, hostId));
        return response;
    }

    private HostAiInsightTab parseTab(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return HostAiInsightTab.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void applyEligibilityResponse(
            HostAiInsightEligibilityResponse response,
            HostAiInsightTab tab,
            HostAiInsightEligibilityResult result
    ) {
        switch (tab) {
            case REVIEW -> response.setReview(result);
            case THEME -> response.setTheme(result);
            case DEMAND -> response.setDemand(result);
        }
    }
}
