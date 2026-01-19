package com.ssg9th2team.geharbang.domain.revenue.host.repository.mybatis;

import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueDetailResponse;
import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueTrendResponse;
import com.ssg9th2team.geharbang.domain.revenue.host.entity.HostRevenueSummaryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HostRevenueMapper {

    HostRevenueSummaryRow selectMonthlyRevenueSummary(
            @Param("hostId") Long hostId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    Long selectExpectedNextMonthRevenue(
            @Param("hostId") Long hostId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<HostRevenueTrendResponse> selectRevenueTrend(
            @Param("hostId") Long hostId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<HostRevenueTrendResponse> selectRevenueTrendFromReservations(
            @Param("hostId") Long hostId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<HostRevenueDetailResponse> selectRevenueDetails(
            @Param("hostId") Long hostId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<HostRevenueDetailResponse> selectRevenueDetailsFromReservations(
            @Param("hostId") Long hostId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<HostRevenueDetailResponse> selectRevenueDailyDetails(
            @Param("hostId") Long hostId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<HostRevenueDetailResponse> selectRevenueDailyDetailsFromReservations(
            @Param("hostId") Long hostId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
