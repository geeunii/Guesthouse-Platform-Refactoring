package com.ssg9th2team.geharbang.domain.review.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class HostReviewResponse {
    private Long reviewId;
    private Long accommodationId;
    private String accommodationName;
    private Double rating;
    private String content;
    private String authorName;
    private String visitDate;
    private LocalDateTime createdAt;
    private Boolean isCrawled;
    private String replyContent;
    private LocalDateTime replyUpdatedAt;
}
