package com.ssg9th2team.geharbang.domain.revenue.host.service;

import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueDetailResponse;
import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueSummaryResponse;
import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueTrendResponse;
import com.ssg9th2team.geharbang.domain.revenue.host.entity.HostRevenueSummaryRow;
import com.ssg9th2team.geharbang.domain.revenue.host.repository.mybatis.HostRevenueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostRevenueServiceImpl implements HostRevenueService {

    private final HostRevenueMapper hostRevenueMapper;

    @Value("${host.platform.fee-rate:0.04}")
    private double platformFeeRate;

    @Override
    public HostRevenueSummaryResponse getSummary(Long hostId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        // Summary uses checkout-based confirmed reservations for revenue totals.
        HostRevenueSummaryRow row = hostRevenueMapper.selectMonthlyRevenueSummary(hostId, start, end);

        long totalRevenue = row == null ? 0L : row.getTotalRevenue();
        int reservationCount = row == null ? 0 : row.getReservationCount();

        LocalDate nextStart = end;
        LocalDate nextEnd = nextStart.plusMonths(1);
        // Expected revenue uses next month's checkout window.
        Long expectedNextMonthRevenue = hostRevenueMapper.selectExpectedNextMonthRevenue(hostId, nextStart, nextEnd);

        // Fee uses a configurable platform rate (default 4%).
        long platformFeeAmount = calculatePlatformFee(totalRevenue, platformFeeRate);

        return HostRevenueSummaryResponse.builder()
                .year(year)
                .month(month)
                .totalRevenue(totalRevenue)
                .expectedNextMonthRevenue(expectedNextMonthRevenue == null ? 0L : expectedNextMonthRevenue)
                .platformFeeRate(platformFeeRate)
                .platformFeeAmount(platformFeeAmount)
                .reservationCount(reservationCount)
                .build();
    }

    @Override
    public List<HostRevenueTrendResponse> getTrend(Long hostId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = start.plusYears(1);
        // Monthly trend is sourced from host_daily_stats for performance.
        List<HostRevenueTrendResponse> raw = hostRevenueMapper.selectRevenueTrend(hostId, start, end);
        if (raw == null || raw.isEmpty() || !hasNonZeroTrend(raw)) {
            raw = hostRevenueMapper.selectRevenueTrendFromReservations(hostId, start, end);
        }
        Map<Integer, HostRevenueTrendResponse> byMonth = raw.stream()
                .collect(Collectors.toMap(HostRevenueTrendResponse::getMonth, item -> item));

        // Fill missing months with zero values to keep a stable 1-12 series.
        return buildMonthlyTrend(year, byMonth);
    }

    @Override
    public List<HostRevenueDetailResponse> getDetails(Long hostId, LocalDate from, LocalDate to, String granularity) {
        LocalDate end = to.plusDays(1);
        String normalized = granularity == null ? "month" : granularity.toLowerCase();

        if ("day".equals(normalized)) {
            List<HostRevenueDetailResponse> raw = hostRevenueMapper.selectRevenueDailyDetails(hostId, from, end);
            if (raw == null || raw.isEmpty() || !hasNonZeroDetails(raw)) {
                raw = hostRevenueMapper.selectRevenueDailyDetailsFromReservations(hostId, from, end);
            }
            Map<String, HostRevenueDetailResponse> byPeriod = new HashMap<>();
            for (HostRevenueDetailResponse item : raw) {
                byPeriod.put(item.getPeriod(), item);
            }
            return buildDailyDetails(from, to, byPeriod);
        }

        List<HostRevenueDetailResponse> raw = hostRevenueMapper.selectRevenueDetails(hostId, from, end);
        if (raw == null || raw.isEmpty() || !hasNonZeroDetails(raw)) {
            raw = hostRevenueMapper.selectRevenueDetailsFromReservations(hostId, from, end);
        }
        Map<String, HostRevenueDetailResponse> byPeriod = new HashMap<>();
        for (HostRevenueDetailResponse item : raw) {
            byPeriod.put(item.getPeriod(), item);
        }

        // Normalize to a month-by-month list so the UI can render gaps.
        return buildMonthlyDetails(from, to, byPeriod);
    }

    private long calculatePlatformFee(long totalRevenue, double feeRate) {
        return BigDecimal.valueOf(totalRevenue)
                .multiply(BigDecimal.valueOf(feeRate))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private List<HostRevenueTrendResponse> buildMonthlyTrend(int year, Map<Integer, HostRevenueTrendResponse> byMonth) {
        List<HostRevenueTrendResponse> result = new ArrayList<>();
        YearMonth cursor = YearMonth.of(year, 1);
        YearMonth end = YearMonth.of(year, 12);
        while (!cursor.isAfter(end)) {
            HostRevenueTrendResponse existing = byMonth.get(cursor.getMonthValue());
            if (existing != null) {
                result.add(existing);
            } else {
                result.add(new HostRevenueTrendResponse(cursor.getMonthValue(), 0L, 0, 0.0));
            }
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    private List<HostRevenueDetailResponse> buildMonthlyDetails(LocalDate from, LocalDate to,
                                                                Map<String, HostRevenueDetailResponse> byPeriod) {
        List<HostRevenueDetailResponse> result = new ArrayList<>();
        YearMonth cursor = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        while (!cursor.isAfter(end)) {
            String key = String.format("%d-%02d", cursor.getYear(), cursor.getMonthValue());
            HostRevenueDetailResponse existing = byPeriod.get(key);
            if (existing != null) {
                result.add(existing);
            } else {
                result.add(new HostRevenueDetailResponse(key, 0L, 0.0));
            }
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    private List<HostRevenueDetailResponse> buildDailyDetails(LocalDate from, LocalDate to,
                                                              Map<String, HostRevenueDetailResponse> byPeriod) {
        List<HostRevenueDetailResponse> result = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            String key = cursor.toString();
            HostRevenueDetailResponse existing = byPeriod.get(key);
            if (existing != null) {
                result.add(existing);
            } else {
                result.add(new HostRevenueDetailResponse(key, 0L, 0.0));
            }
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private boolean hasNonZeroTrend(List<HostRevenueTrendResponse> rows) {
        return rows.stream().anyMatch(item ->
                item.getRevenue() > 0 || item.getReservationCount() > 0);
    }

    private boolean hasNonZeroDetails(List<HostRevenueDetailResponse> rows) {
        return rows.stream().anyMatch(item -> item.getRevenue() > 0);
    }
}
