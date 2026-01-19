package com.ssg9th2team.geharbang.domain.review.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCreateDto {
    private Long accommodationsId;      // 숙소 ID
    private BigDecimal rating;          // 별점
    private String content;             // 리뷰 내용
    private String visitDate;           // 방문일

    // 연관 데이터
    private List<Long> tagIds;          // 태그 ID 목록
    private List<String> imageUrls;     // 이미지 URL 목록

}
