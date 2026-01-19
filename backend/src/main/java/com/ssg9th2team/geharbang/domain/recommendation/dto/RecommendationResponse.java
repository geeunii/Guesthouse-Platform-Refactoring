package com.ssg9th2team.geharbang.domain.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {
    private Long accommodationId;
    private String accommodationName;
    private String shortDescription;
    private String city;
    private String district;
    private String imageUrl;
    private Double rating;
    private Integer minPrice;
    private Double score;
    private List<String> matchedThemes;
    private List<String> matchedTags;
}
