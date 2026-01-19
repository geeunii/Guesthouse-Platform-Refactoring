package com.ssg9th2team.geharbang.domain.review.host.repository.mybatis;

import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewResponse;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewSummaryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HostReviewMapper {
    HostReviewSummaryResponse selectSummary(@Param("hostId") Long hostId);

    List<HostReviewResponse> selectReviews(
            @Param("hostId") Long hostId,
            @Param("accommodationId") Long accommodationId,
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("sort") String sort
    );

    Long selectReviewIdForHost(
            @Param("hostId") Long hostId,
            @Param("reviewId") Long reviewId
    );
}
