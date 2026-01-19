package com.ssg9th2team.geharbang.domain.report.host.ai;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostAiInsightRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportResponse;
import com.ssg9th2team.geharbang.domain.report.host.service.HostReportService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HostAiInsightEligibilityChecker {

    static final int MIN_REVIEW_REQUIRED = 10;
    static final int RECOMMENDED_REVIEW_COUNT = 20;
    static final int MIN_THEME_RESERVATION_REQUIRED = 10;
    static final int RECOMMENDED_THEME_RESERVATION = 20;
    static final int MIN_THEME_REVENUE_RESERVATION_REQUIRED = 5;
    static final int RECOMMENDED_THEME_REVENUE_RESERVATION = 15;
    static final int MIN_THEME_DISTINCT = 2;
    static final int MIN_DEMAND_POINTS = 14;
    static final int RECOMMENDED_DEMAND_POINTS = 28;

    private final HostReportService hostReportService;

    public HostAiInsightEligibilityResult evaluate(@NotNull HostAiInsightTab tab, HostAiInsightRequest request, Long hostId) {
        return switch (tab) {
            case REVIEW -> evaluateReviewEligibility(request, hostId);
            case THEME -> evaluateThemeEligibility(request, hostId);
            case DEMAND -> evaluateDemandEligibility(request, hostId);
        };
    }

    private HostAiInsightEligibilityResult evaluateReviewEligibility(HostAiInsightRequest request, Long hostId) {
        HostReviewReportSummaryResponse summary = hostReportService.getReviewSummary(
                hostId,
                request.getAccommodationId(),
                request.getFrom(),
                request.getTo()
        );
        int current = summary.getReviewCount() != null ? summary.getReviewCount() : 0;
        if (current < MIN_REVIEW_REQUIRED) {
            return HostAiInsightEligibilityResult.blocked(
                    String.format("AI 요약은 리뷰 %d건 이상부터 생성할 수 있어요. (현재 %d건)", MIN_REVIEW_REQUIRED, current),
                    current,
                    MIN_REVIEW_REQUIRED,
                    RECOMMENDED_REVIEW_COUNT,
                    "건"
            );
        }
        if (current < RECOMMENDED_REVIEW_COUNT) {
            return HostAiInsightEligibilityResult.warn(
                    String.format("표본이 적어 참고용입니다. (현재 %d건 / 권장 %d건)", current, RECOMMENDED_REVIEW_COUNT),
                    current,
                    MIN_REVIEW_REQUIRED,
                    RECOMMENDED_REVIEW_COUNT,
                    "건"
            );
        }
        return HostAiInsightEligibilityResult.ok(current, MIN_REVIEW_REQUIRED, RECOMMENDED_REVIEW_COUNT, "건");
    }

    private HostAiInsightEligibilityResult evaluateThemeEligibility(HostAiInsightRequest request, Long hostId) {
        HostThemeReportResponse report = hostReportService.getThemePopularity(
                hostId,
                request.getAccommodationId(),
                request.getFrom(),
                request.getTo(),
                request.getMetric()
        );
        int distinctThemes = report.getRows() == null ? 0 : report.getRows().size();
        int reservationCount = report.getRows() == null ? 0 : report.getRows().stream()
                .mapToInt(row -> row.getReservationCount() != null ? row.getReservationCount().intValue() : 0)
                .sum();
        double revenueSum = report.getRows() == null ? 0 : report.getRows().stream()
                .mapToDouble(row -> row.getRevenueSum() != null ? row.getRevenueSum().doubleValue() : 0)
                .sum();
        if (distinctThemes < MIN_THEME_DISTINCT) {
            return HostAiInsightEligibilityResult.blocked(
                    String.format("테마가 %d개 이상 필요해요. (현재 %d개)", MIN_THEME_DISTINCT, distinctThemes),
                    reservationCount,
                    MIN_THEME_RESERVATION_REQUIRED,
                    RECOMMENDED_THEME_RESERVATION,
                    "건"
            );
        }
        if (reservationCount == 0) {
            return HostAiInsightEligibilityResult.blocked(
                    "AI 요약은 기간 예약 데이터가 필요해요. (현재 0건)",
                    reservationCount,
                    MIN_THEME_RESERVATION_REQUIRED,
                    RECOMMENDED_THEME_RESERVATION,
                    "건"
            );
        }

        String metric = report.getMetric();
        if ("revenue".equals(metric)) {
            if (revenueSum == 0 || reservationCount < MIN_THEME_REVENUE_RESERVATION_REQUIRED) {
                String message = revenueSum == 0
                        ? "AI 요약은 기간 매출 데이터가 필요해요. (현재 0원)"
                        : String.format("AI 요약은 예약 %d건 이상이 필요해요. (현재 %d건)",
                        MIN_THEME_REVENUE_RESERVATION_REQUIRED, reservationCount);
                return HostAiInsightEligibilityResult.blocked(
                        message,
                        reservationCount,
                        MIN_THEME_REVENUE_RESERVATION_REQUIRED,
                        RECOMMENDED_THEME_REVENUE_RESERVATION,
                        "건"
                );
            }
            if (reservationCount < RECOMMENDED_THEME_REVENUE_RESERVATION) {
                return HostAiInsightEligibilityResult.warn(
                        String.format("표본이 적어 참고용입니다. (현재 %d건 / 권장 %d건)",
                                reservationCount, RECOMMENDED_THEME_REVENUE_RESERVATION),
                        reservationCount,
                        MIN_THEME_REVENUE_RESERVATION_REQUIRED,
                        RECOMMENDED_THEME_REVENUE_RESERVATION,
                        "건"
                );
            }
            return HostAiInsightEligibilityResult.ok(
                    reservationCount,
                    MIN_THEME_REVENUE_RESERVATION_REQUIRED,
                    RECOMMENDED_THEME_REVENUE_RESERVATION,
                    "건"
            );
        }

        if (reservationCount < MIN_THEME_RESERVATION_REQUIRED) {
            return HostAiInsightEligibilityResult.blocked(
                    String.format("AI 요약은 예약 %d건 이상이 필요해요. (현재 %d건)",
                            MIN_THEME_RESERVATION_REQUIRED, reservationCount),
                    reservationCount,
                    MIN_THEME_RESERVATION_REQUIRED,
                    RECOMMENDED_THEME_RESERVATION,
                    "건"
            );
        }
        if (reservationCount < RECOMMENDED_THEME_RESERVATION) {
            return HostAiInsightEligibilityResult.warn(
                    String.format("표본이 적어 참고용입니다. (현재 %d건 / 권장 %d건)",
                            reservationCount, RECOMMENDED_THEME_RESERVATION),
                    reservationCount,
                    MIN_THEME_RESERVATION_REQUIRED,
                    RECOMMENDED_THEME_RESERVATION,
                    "건"
            );
        }
        return HostAiInsightEligibilityResult.ok(
                reservationCount,
                MIN_THEME_RESERVATION_REQUIRED,
                RECOMMENDED_THEME_RESERVATION,
                "건"
        );
    }

    private HostAiInsightEligibilityResult evaluateDemandEligibility(HostAiInsightRequest request, Long hostId) {
        int historyDays = request.getHistoryDays() != null && request.getHistoryDays() > 0
                ? request.getHistoryDays()
                : 180;
        int current = hostReportService.countDemandHistoryPoints(
                hostId,
                request.getAccommodationId(),
                historyDays
        );
        if (current < MIN_DEMAND_POINTS) {
            return HostAiInsightEligibilityResult.blocked(
                    String.format("예측을 위해 일별 관측치 %d일 이상이 필요해요. (현재 %d일 / 권장 %d일)",
                            MIN_DEMAND_POINTS, current, RECOMMENDED_DEMAND_POINTS),
                    current,
                    MIN_DEMAND_POINTS,
                    RECOMMENDED_DEMAND_POINTS,
                    "일"
            );
        }
        if (current < RECOMMENDED_DEMAND_POINTS) {
            return HostAiInsightEligibilityResult.warn(
                    String.format("표본이 적어 참고용입니다. (현재 %d일 / 권장 %d일)",
                            current, RECOMMENDED_DEMAND_POINTS),
                    current,
                    MIN_DEMAND_POINTS,
                    RECOMMENDED_DEMAND_POINTS,
                    "일"
            );
        }
        return HostAiInsightEligibilityResult.ok(
                current,
                MIN_DEMAND_POINTS,
                RECOMMENDED_DEMAND_POINTS,
                "일"
        );
    }
}
