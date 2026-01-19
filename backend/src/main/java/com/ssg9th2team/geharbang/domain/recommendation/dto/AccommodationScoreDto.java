package com.ssg9th2team.geharbang.domain.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationScoreDto {
    private Long accommodationId;
    private String accommodationName;
    private String shortDescription;
    private String city;
    private String district;
    private String imageUrl;
    private Double rating;
    private Double minPrice;
    private Integer themeMatchCount;
    private Integer tagMatchCount;
    private Integer reviewCount; // 베이지안 평점용
    private Integer totalThemeCount; // 테마 Jaccard 유사도용 ('숙소'가 가진 전체 테마 수)
    private Integer reservationCount; // 인기도 점수용 (예약 수)
}
