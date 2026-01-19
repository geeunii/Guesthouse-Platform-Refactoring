package com.ssg9th2team.geharbang.domain.report.host.repository.mybatis;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostDemandDailyRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportRatingRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportRecentRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportTagRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportTrendRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface HostReportMapper {
    Long selectHostAccommodationId(@Param("hostId") Long hostId, @Param("accommodationId") Long accommodationId);

    HostReviewReportSummaryResponse selectReviewSummary(
            @Param("hostId") Long hostId,
            @Param("accommodationId") Long accommodationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<HostReviewReportRatingRow> selectReviewRatingDistribution(
            @Param("hostId") Long hostId,
            @Param("accommodationId") Long accommodationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<HostReviewReportTagRow> selectTopReviewTags(
            @Param("hostId") Long hostId,
            @Param("accommodationId") Long accommodationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<HostReviewReportRecentRow> selectRecentReviews(
            @Param("hostId") Long hostId,
            @Param("accommodationId") Long accommodationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit
    );

    List<HostReviewReportTrendRow> selectReviewTrend(
            @Param("hostId") Long hostId,
            @Param("accommodationId") Long accommodationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<HostThemeReportRow> selectThemePopularity(
            @Param("hostId") Long hostId,
            @Param("accommodationId") Long accommodationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("metric") String metric
    );

    List<HostDemandDailyRow> selectDemandDaily(
            @Param("hostId") Long hostId,
            @Param("accommodationId") Long accommodationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
