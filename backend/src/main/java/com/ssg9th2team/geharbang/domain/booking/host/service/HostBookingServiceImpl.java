package com.ssg9th2team.geharbang.domain.booking.host.service;

import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingResponse;
import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingCursorInfo;
import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingListMeta;
import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingListResponse;
import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingPageInfo;
import com.ssg9th2team.geharbang.domain.booking.host.repository.mybatis.HostBookingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostBookingServiceImpl implements HostBookingService {

    private final HostBookingMapper hostBookingMapper;

    @Override
    public HostBookingListResponse listBookings(
            Long hostUserId,
            YearMonth month,
            String viewClass,
            LocalDate startDate,
            LocalDate endDate,
            boolean upcomingOnly,
            String sort,
            String rangeMode,
            Integer page,
            Integer size,
            String cursor
    ) {
        PagingAndRangeParams params = resolvePagingAndRangeParams(viewClass, page, size, rangeMode);
        DateRange range = resolveDateRange(month, startDate, endDate, upcomingOnly);
        LocalDateTime start = range.startInclusive != null ? range.startInclusive.atStartOfDay() : null;
        LocalDateTime end = range.endExclusive != null ? range.endExclusive.atStartOfDay() : null;
        String appliedSort = resolveSort(sort, upcomingOnly || range.startInclusive != null);
        HostBookingListMeta meta = buildMeta(appliedSort, params.appliedRangeMode, range.startInclusive, range.endExclusive);

        if (params.isCalendarView) {
            return handleCalendarView(hostUserId, start, end, upcomingOnly, params.appliedRangeMode, appliedSort, meta);
        }

        boolean useCursor = cursor != null || page == null;
        if (useCursor) {
            return handleCursorListView(
                    hostUserId,
                    start,
                    end,
                    upcomingOnly,
                    params.appliedRangeMode,
                    appliedSort,
                    params.safeSize,
                    cursor,
                    meta
            );
        }

        return handlePagedListView(
                hostUserId,
                start,
                end,
                upcomingOnly,
                params.appliedRangeMode,
                appliedSort,
                params.safeSize,
                params.safePage,
                meta
        );
    }

    @Override
    public HostBookingResponse getBooking(Long hostUserId, Long reservationId) {
        return hostBookingMapper.selectHostBookingById(hostUserId, reservationId);
    }

    private String resolveSort(String sort, boolean hasRange) {
        if ("CREATED_DESC".equalsIgnoreCase(sort)) return "CREATED_DESC";
        if ("CHECKIN_ASC".equalsIgnoreCase(sort)) return "CHECKIN_ASC";
        return hasRange ? "CHECKIN_ASC" : "CREATED_DESC";
    }

    private PagingAndRangeParams resolvePagingAndRangeParams(String viewClass, Integer page, Integer size, String rangeMode) {
        int safeSize = size != null && size > 0 ? size : 20;
        int maxSize = 50;
        boolean isCalendarView = viewClass != null && viewClass.equalsIgnoreCase("calendar");
        if (!isCalendarView) {
            safeSize = Math.min(safeSize, maxSize);
        }
        int safePage = page != null && page >= 0 ? page : 0;
        String appliedRangeMode = "START".equalsIgnoreCase(rangeMode) ? "START" : "OVERLAP";
        return new PagingAndRangeParams(safeSize, safePage, maxSize, isCalendarView, appliedRangeMode);
    }

    private DateRange resolveDateRange(YearMonth month, LocalDate startDate, LocalDate endDate, boolean upcomingOnly) {
        LocalDate startInclusive = null;
        LocalDate endExclusive = null;
        if (month != null) {
            startInclusive = month.atDay(1);
            endExclusive = month.plusMonths(1).atDay(1);
        } else if (startDate != null || endDate != null) {
            startInclusive = startDate != null ? startDate : LocalDate.now();
            endExclusive = endDate != null ? endDate.plusDays(1) : startInclusive.plusDays(1);
        } else if (upcomingOnly) {
            startInclusive = startDate != null ? startDate : LocalDate.now();
        }
        return new DateRange(startInclusive, endExclusive);
    }

    private HostBookingListMeta buildMeta(String appliedSort, String appliedRangeMode, LocalDate startInclusive, LocalDate endExclusive) {
        HostBookingListMeta meta = new HostBookingListMeta();
        meta.setAppliedSort(appliedSort);
        meta.setRangeMode(appliedRangeMode);
        meta.setRangeFrom(startInclusive);
        meta.setRangeTo(endExclusive != null ? endExclusive.minusDays(1) : null);
        return meta;
    }

    private HostBookingListResponse handleCalendarView(
            Long hostUserId,
            LocalDateTime start,
            LocalDateTime end,
            boolean upcomingOnly,
            String appliedRangeMode,
            String appliedSort,
            HostBookingListMeta meta
    ) {
        List<HostBookingResponse> items = hostBookingMapper.selectHostBookingsAll(
                hostUserId,
                start,
                end,
                upcomingOnly,
                appliedRangeMode,
                appliedSort
        );
        HostBookingListResponse response = new HostBookingListResponse();
        response.setItems(items);
        response.setMeta(meta);
        return response;
    }

    private HostBookingListResponse handleCursorListView(
            Long hostUserId,
            LocalDateTime start,
            LocalDateTime end,
            boolean upcomingOnly,
            String appliedRangeMode,
            String appliedSort,
            int safeSize,
            String cursor,
            HostBookingListMeta meta
    ) {
        CursorValue cursorValue = (cursor != null && !cursor.isBlank()) ? parseCursor(cursor) : new CursorValue(null, null);
        List<HostBookingResponse> raw = hostBookingMapper.selectHostBookingsCursor(
                hostUserId,
                start,
                end,
                upcomingOnly,
                appliedRangeMode,
                appliedSort,
                cursorValue.value,
                cursorValue.id,
                safeSize + 1
        );
        boolean hasNext = raw.size() > safeSize;
        List<HostBookingResponse> items = hasNext ? raw.subList(0, safeSize) : raw;
        HostBookingCursorInfo cursorInfo = new HostBookingCursorInfo();
        cursorInfo.setHasNext(hasNext);
        cursorInfo.setNextCursor(hasNext ? buildCursor(items.get(items.size() - 1), appliedSort) : null);
        meta.setCursorInfo(cursorInfo);
        HostBookingListResponse response = new HostBookingListResponse();
        response.setItems(items);
        response.setMeta(meta);
        return response;
    }

    private HostBookingListResponse handlePagedListView(
            Long hostUserId,
            LocalDateTime start,
            LocalDateTime end,
            boolean upcomingOnly,
            String appliedRangeMode,
            String appliedSort,
            int safeSize,
            int safePage,
            HostBookingListMeta meta
    ) {
        int offset = safePage * safeSize;
        List<HostBookingResponse> items = hostBookingMapper.selectHostBookingsPaged(
                hostUserId,
                start,
                end,
                upcomingOnly,
                appliedRangeMode,
                appliedSort,
                safeSize,
                offset
        );
        long total = hostBookingMapper.countHostBookings(hostUserId, start, end, upcomingOnly, appliedRangeMode);
        HostBookingPageInfo pageInfo = new HostBookingPageInfo();
        pageInfo.setPage(safePage);
        pageInfo.setSize(safeSize);
        pageInfo.setTotalElements(total);
        pageInfo.setTotalPages(total == 0 ? 0 : (int) Math.ceil((double) total / safeSize));
        meta.setPageInfo(pageInfo);

        HostBookingListResponse response = new HostBookingListResponse();
        response.setItems(items);
        response.setMeta(meta);
        return response;
    }

    private CursorValue parseCursor(String cursor) {
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|");
            if (parts.length != 2) return new CursorValue(null, null);
            LocalDateTime value = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            Long id = Long.parseLong(parts[1]);
            return new CursorValue(value, id);
        } catch (Exception ex) {
            return new CursorValue(null, null);
        }
    }

    private String buildCursor(HostBookingResponse item, String sort) {
        LocalDateTime value = "CREATED_DESC".equals(sort) ? item.getCreatedAt() : item.getCheckin();
        if (value == null || item.getReservationId() == null) return null;
        String raw = value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + item.getReservationId();
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private record PagingAndRangeParams(int safeSize, int safePage, int maxSize, boolean isCalendarView, String appliedRangeMode) {
    }

    private record DateRange(LocalDate startInclusive, LocalDate endExclusive) {
    }

    private record CursorValue(LocalDateTime value, Long id) {
    }
}
