package com.ssg9th2team.geharbang.domain.review.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewUpdateDto {
    private String content;             // 수정할 리뷰 내용
    private List<String> imageUrls;     // 수정할 이미지 URL 목록 (기존 이미지 포함, 전체 교체 방식)
    private Double rating;              // 수정할 별점
    private List<Long> tagIds;          // 수정할 태그 ID 목록
}
