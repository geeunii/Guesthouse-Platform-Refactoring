package com.ssg9th2team.geharbang.domain.recommendation.service;

import com.ssg9th2team.geharbang.domain.recommendation.dto.RecommendationResponse;

import java.util.List;

public interface RecommendationService {

    /**
     * 사용자 맞춤 숙소 추천
     * 
     * @param userId 사용자 ID
     * @param limit  추천 개수
     * @return 추천 숙소 목록
     */
    List<RecommendationResponse> getRecommendations(Long userId, int limit);
}
