package com.ssg9th2team.geharbang.domain.review.host.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class HostReviewReplyResponse {
    private final Long reviewId;
    private final String content;
    private final LocalDateTime updatedAt;
}
