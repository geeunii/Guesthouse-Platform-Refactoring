package com.ssg9th2team.geharbang.domain.report.host.service;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportResponse;
import com.ssg9th2team.geharbang.domain.report.host.forecast.HostDemandForecastCalculator;
import com.ssg9th2team.geharbang.domain.report.host.forecast.HostDemandForecastResult;
import com.ssg9th2team.geharbang.domain.report.host.repository.mybatis.HostReportMapper;
import com.ssg9th2team.geharbang.domain.report.host.ai.AiSummaryClient;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportRatingRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportRecentRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportTagRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportTrendRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostDemandDailyRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportRow;
import com.ssg9th2team.geharbang.domain.holiday.service.HolidayService;
import com.ssg9th2team.geharbang.domain.holiday.dto.HolidayItemResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HostReportService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_RECENT_DAYS = 30;

    private final HostReportMapper hostReportMapper;
    private final AiSummaryClient aiSummaryClient;
    private final HolidayService holidayService;
    private final HostDemandForecastCalculator forecastCalculator = new HostDemandForecastCalculator();

    public HostReportService(
            HostReportMapper hostReportMapper,
            @Qualifier("aiSummaryClientFacade") AiSummaryClient aiSummaryClient,
            HolidayService holidayService
    ) {
        this.hostReportMapper = hostReportMapper;
        this.aiSummaryClient = aiSummaryClient;
        this.holidayService = holidayService;
    }

    public HostReviewReportSummaryResponse getReviewSummary(Long hostId, Long accommodationId, LocalDate from, LocalDate to) {
        validateOwnership(hostId, accommodationId);

        LocalDate today = LocalDate.now(KST);
        LocalDate safeTo = to != null ? to : today;
        LocalDate safeFrom = from != null ? from : safeTo.minusDays(DEFAULT_RECENT_DAYS);
        LocalDateTime start = safeFrom.atStartOfDay();
        LocalDateTime end = safeTo.plusDays(1).atStartOfDay();

        HostReviewReportSummaryResponse summary = hostReportMapper.selectReviewSummary(hostId, accommodationId, start, end);
        if (summary == null) {
            summary = new HostReviewReportSummaryResponse();
            summary.setAvgRating(0.0);
            summary.setReviewCount(0);
        }

        List<HostReviewReportRatingRow> ratingRows = hostReportMapper.selectReviewRatingDistribution(hostId, accommodationId, start, end);
        Map<Integer, Integer> ratingDistribution = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            ratingDistribution.put(rating, 0);
        }
        if (ratingRows != null) {
            for (HostReviewReportRatingRow row : ratingRows) {
                if (row.getRating() == null) continue;
                ratingDistribution.put(row.getRating(), row.getCount() != null ? row.getCount() : 0);
            }
        }

        List<HostReviewReportTagRow> tags = hostReportMapper.selectTopReviewTags(hostId, accommodationId, start, end);
        // AI 분석을 위해 최근 리뷰를 더 많이 가져오도록 수정 (5 -> 50)
        List<HostReviewReportRecentRow> recentReviews = hostReportMapper.selectRecentReviews(hostId, accommodationId, start, end, 50);

        summary.setFrom(safeFrom);
        summary.setTo(safeTo);
        summary.setAccommodationId(accommodationId);
        summary.setRatingDistribution(ratingDistribution);
        summary.setTopTags(tags == null ? List.of() : tags);
        summary.setRecentReviews(recentReviews == null ? List.of() : recentReviews);

        return summary;
    }

    public List<HostReviewReportTrendRow> getReviewTrend(Long hostId, Long accommodationId, int months) {
        validateOwnership(hostId, accommodationId);

        int safeMonths = months > 0 ? months : 6;
        LocalDate today = LocalDate.now(KST);
        LocalDate startMonth = today.minusMonths(safeMonths - 1L).withDayOfMonth(1);
        LocalDate endMonth = today.plusMonths(1).withDayOfMonth(1);
        LocalDateTime start = startMonth.atStartOfDay();
        LocalDateTime end = endMonth.atStartOfDay();

        List<HostReviewReportTrendRow> rows = hostReportMapper.selectReviewTrend(hostId, accommodationId, start, end);
        Map<String, HostReviewReportTrendRow> rowMap = new LinkedHashMap<>();
        if (rows != null) {
            for (HostReviewReportTrendRow row : rows) {
                if (row.getPeriod() != null) {
                    rowMap.put(row.getPeriod(), row);
                }
            }
        }

        List<HostReviewReportTrendRow> result = new ArrayList<>();
        LocalDate cursor = startMonth;
        while (cursor.isBefore(endMonth)) {
            String key = String.format(Locale.KOREA, "%d-%02d", cursor.getYear(), cursor.getMonthValue());
            HostReviewReportTrendRow row = rowMap.getOrDefault(key, null);
            if (row == null) {
                row = new HostReviewReportTrendRow();
                row.setPeriod(key);
                row.setReviewCount(0);
                row.setAvgRating(0.0);
            }
            result.add(row);
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    public HostThemeReportResponse getThemePopularity(Long hostId, Long accommodationId, LocalDate from, LocalDate to, String metric) {
        validateOwnership(hostId, accommodationId);

        LocalDate today = LocalDate.now(KST);
        LocalDate safeTo = to != null ? to : today;
        LocalDate safeFrom = from != null ? from : safeTo.minusDays(DEFAULT_RECENT_DAYS);
        LocalDateTime start = safeFrom.atStartOfDay();
        LocalDateTime end = safeTo.plusDays(1).atStartOfDay();

        String safeMetric = "revenue".equalsIgnoreCase(metric) ? "revenue" : "reservations";
        List<HostThemeReportRow> rows = hostReportMapper.selectThemePopularity(hostId, accommodationId, start, end, safeMetric);

        HostThemeReportResponse response = new HostThemeReportResponse();
        response.setFrom(safeFrom);
        response.setTo(safeTo);
        response.setAccommodationId(accommodationId);
        response.setMetric(safeMetric);
        response.setRows(rows == null ? List.of() : rows);
        return response;
    }

    public HostForecastResponse getDemandForecast(Long hostId, Long accommodationId, String target, int horizonDays, int historyDays) {
        validateOwnership(hostId, accommodationId);

        int safeHistoryDays = historyDays > 0 ? historyDays : 180;
        int safeHorizonDays = horizonDays > 0 ? horizonDays : 30;
        String safeTarget = "revenue".equalsIgnoreCase(target) ? "revenue" : "reservations";

        LocalDate today = LocalDate.now(KST);
        LocalDate historyStart = today.minusDays(safeHistoryDays);
        LocalDate historyEnd = today.plusDays(1);
        LocalDate forecastEnd = today.plusDays(safeHorizonDays);

        List<HostDemandDailyRow> rows = hostReportMapper.selectDemandDaily(
                hostId,
                accommodationId,
                historyStart.atStartOfDay(),
                historyEnd.atStartOfDay()
        );

        Map<LocalDate, Double> dailyValues = new LinkedHashMap<>();
        for (int i = 0; i <= safeHistoryDays; i++) {
            LocalDate date = historyStart.plusDays(i);
            dailyValues.put(date, 0.0);
        }
        int historyPointCount = 0;
        if (rows != null) {
            Set<LocalDate> observedDates = new HashSet<>();
            for (HostDemandDailyRow row : rows) {
                LocalDate date = row.getStatDate();
                if (date == null || !dailyValues.containsKey(date)) continue;
                double value = "revenue".equals(safeTarget)
                        ? row.getRevenue() != null ? row.getRevenue() : 0
                        : row.getReservationCount() != null ? row.getReservationCount() : 0;
                dailyValues.put(date, value);
                observedDates.add(date);
            }
            historyPointCount = observedDates.size();
        }

        Set<LocalDate> holidays = loadHolidaySet(historyStart, forecastEnd);
        HostDemandForecastResult computed = forecastCalculator.generate(
                dailyValues,
                holidays,
                today,
                safeHorizonDays,
                safeHistoryDays
        );

        HostForecastResponse response = new HostForecastResponse();
        response.setTarget(safeTarget);
        response.setHorizonDays(safeHorizonDays);
        response.setHistoryDays(safeHistoryDays);
        response.setFrom(historyStart);
        response.setTo(today);
        response.setAccommodationId(accommodationId);
        response.setModelVersion(computed.getModelVersion());
        response.setExplain(computed.getExplain());
        response.setDiagnostics(computed.getDiagnostics());
        response.setBaseline(computed.getBaseline());
        response.setForecastDaily(computed.getDaily());
        response.setForecastSummary(computed.getSummary());
        response.setHistoryPointCount(historyPointCount);
        return response;
    }

    public int countDemandHistoryPoints(Long hostId, Long accommodationId, int historyDays) {
        validateOwnership(hostId, accommodationId);
        int safeHistoryDays = historyDays > 0 ? historyDays : 180;
        LocalDate today = LocalDate.now(KST);
        LocalDate historyStart = today.minusDays(safeHistoryDays);
        LocalDate historyEnd = today.plusDays(1);
        List<HostDemandDailyRow> rows = hostReportMapper.selectDemandDaily(
                hostId,
                accommodationId,
                historyStart.atStartOfDay(),
                historyEnd.atStartOfDay()
        );
        if (rows == null) return 0;
        Set<LocalDate> observedDates = new HashSet<>();
        for (HostDemandDailyRow row : rows) {
            if (row == null || row.getStatDate() == null) continue;
            observedDates.add(row.getStatDate());
        }
        return observedDates.size();
    }

    public HostReviewAiSummaryResponse getReviewAiSummary(Long hostId, Long accommodationId, LocalDate from, LocalDate to) {
        HostReviewReportSummaryResponse summary = getReviewSummary(hostId, accommodationId, from, to);
        HostReviewAiSummaryRequest request = new HostReviewAiSummaryRequest();
        request.setAccommodationId(accommodationId);
        request.setFrom(summary.getFrom());
        request.setTo(summary.getTo());
        return aiSummaryClient.generate(summary, request);
    }

    private void validateOwnership(Long hostId, Long accommodationId) {
        if (accommodationId == null) return;
        Long id = hostReportMapper.selectHostAccommodationId(hostId, accommodationId);
        if (id == null) {
            throw new AccessDeniedException("Not allowed to access this accommodation");
        }
    }

    private Set<LocalDate> loadHolidaySet(LocalDate from, LocalDate to) {
        Set<LocalDate> holidays = new HashSet<>();
        LocalDate cursor = from.withDayOfMonth(1);
        LocalDate endCursor = to.withDayOfMonth(1);

        while (!cursor.isAfter(endCursor)) {
            try {
                List<HolidayItemResponse> items = holidayService.getHolidays(cursor.getYear(), cursor.getMonthValue());
                if (items != null) {
                    for (HolidayItemResponse item : items) {
                        if (item == null || item.getDate() == null || !item.isHoliday()) continue;
                        LocalDate date = LocalDate.parse(item.getDate());
                        if ((date.isAfter(from) || date.isEqual(from)) && (date.isBefore(to) || date.isEqual(to))) {
                            holidays.add(date);
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to fetch holidays for {}-{}, continuing without them.", cursor.getYear(), cursor.getMonthValue(), ex);
            }
            cursor = cursor.plusMonths(1);
        }

        return holidays;
    }
}
