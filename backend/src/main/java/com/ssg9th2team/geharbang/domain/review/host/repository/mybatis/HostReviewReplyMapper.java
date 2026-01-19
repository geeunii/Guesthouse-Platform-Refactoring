package com.ssg9th2team.geharbang.domain.review.host.repository.mybatis;

import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewReplyRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HostReviewReplyMapper {
    HostReviewReplyRow selectReplyByReviewId(@Param("reviewId") Long reviewId);

    int insertReply(
            @Param("reviewId") Long reviewId,
            @Param("userId") Long userId,
            @Param("content") String content
    );

    int updateReply(
            @Param("replyId") Long replyId,
            @Param("content") String content
    );
}
