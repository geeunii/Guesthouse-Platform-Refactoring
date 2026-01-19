package com.ssg9th2team.geharbang.domain.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiRecommendationRequest(
        @NotBlank(message = "검색어를 입력해주세요") @Size(max = 500, message = "검색어는 500자 이내로 입력해주세요") String query) {
}
