package com.ssg9th2team.geharbang.domain.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDto {
    private Long reviewId;
    private Long accommodationsId;
    private Long userId;
    private String authorName;
    private Double rating;
    private String content;
    private LocalDateTime createdAt;
    private String replyContent;
    private LocalDateTime replyUpdatedAt;
    private List<ReviewImageDto> images;
    private List<ReviewTagDto> tags;
    private String visitDate;
    private String accommodationName;
    private String accommodationImage;

    private LocalDateTime checkin;
    private LocalDateTime checkout;
}
