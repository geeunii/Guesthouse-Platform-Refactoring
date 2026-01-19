package com.ssg9th2team.geharbang.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiRecommendationResponse {

    private String query; // 원본 검색어
    private List<String> matchedThemes; // 매칭된 테마 목록
    private String reasoning; // AI 추론 설명
    private Double confidence; // 신뢰도 (0~1)
    private List<AccommodationSummary> accommodations; // 추천 숙소 목록

    @Getter
    @Builder
    public static class AccommodationSummary {
        private Long accommodationsId;
        private String accommodationsName;
        private String city;
        private String district;
        private Double rating;
        private Integer reviewCount;
        private String thumbnailUrl;
        private Integer minPrice;
        private List<String> themes; // 숙소의 테마 목록
    }
}
