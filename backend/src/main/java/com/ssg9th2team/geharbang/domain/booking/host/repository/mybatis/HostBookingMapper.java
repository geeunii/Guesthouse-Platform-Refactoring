package com.ssg9th2team.geharbang.domain.booking.host.repository.mybatis;

import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface HostBookingMapper {
    List<HostBookingResponse> selectHostBookingsPaged(
            @Param("hostUserId") Long hostUserId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("upcomingOnly") boolean upcomingOnly,
            @Param("rangeMode") String rangeMode,
            @Param("sort") String sort,
            @Param("size") int size,
            @Param("offset") int offset
    );

    List<HostBookingResponse> selectHostBookingsCursor(
            @Param("hostUserId") Long hostUserId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("upcomingOnly") boolean upcomingOnly,
            @Param("rangeMode") String rangeMode,
            @Param("sort") String sort,
            @Param("cursorValue") LocalDateTime cursorValue,
            @Param("cursorId") Long cursorId,
            @Param("size") int size
    );

    List<HostBookingResponse> selectHostBookingsAll(
            @Param("hostUserId") Long hostUserId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("upcomingOnly") boolean upcomingOnly,
            @Param("rangeMode") String rangeMode,
            @Param("sort") String sort
    );

    long countHostBookings(
            @Param("hostUserId") Long hostUserId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("upcomingOnly") boolean upcomingOnly,
            @Param("rangeMode") String rangeMode
    );

    HostBookingResponse selectHostBookingById(
            @Param("hostUserId") Long hostUserId,
            @Param("reservationId") Long reservationId
    );
}
