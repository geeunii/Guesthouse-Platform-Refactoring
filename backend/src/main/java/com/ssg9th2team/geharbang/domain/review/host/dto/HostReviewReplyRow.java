package com.ssg9th2team.geharbang.domain.review.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class HostReviewReplyRow {
    private Long replyId;
    private Long reviewId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
