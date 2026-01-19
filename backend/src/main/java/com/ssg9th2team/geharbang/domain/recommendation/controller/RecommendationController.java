package com.ssg9th2team.geharbang.domain.recommendation.controller;

import com.ssg9th2team.geharbang.domain.recommendation.dto.AiRecommendationRequest;
import com.ssg9th2team.geharbang.domain.recommendation.dto.AiRecommendationResponse;
import com.ssg9th2team.geharbang.domain.recommendation.dto.RecommendationResponse;
import com.ssg9th2team.geharbang.domain.recommendation.service.AiRecommendationService;
import com.ssg9th2team.geharbang.domain.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final AiRecommendationService aiRecommendationService;

    /**
     * 사용자 맞춤 숙소 추천 API
     * GET /api/recommendations?userId={userId}&limit={limit}
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRecommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(userId, limit);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", recommendations.size(),
                "recommendations", recommendations));
    }

    /**
     * AI 자연어 기반 숙소 추천 API
     * POST /api/recommendations/ai
     * 
     * @param request 사용자 자연어 입력 (예: "조용한 곳에서 풍경 보면서 여행하고 싶어")
     * @return 추천 숙소 목록과 AI 분석 결과
     */
    @PostMapping("/ai")
    public ResponseEntity<AiRecommendationResponse> getAiRecommendations(
            @Valid @RequestBody AiRecommendationRequest request) {

        log.info("AI 추천 요청: {}", request.query());

        AiRecommendationResponse response = aiRecommendationService.recommend(request.query());

        log.info("AI 추천 결과: 테마={}, 숙소 수={}", response.getMatchedThemes(), response.getAccommodations().size());

        return ResponseEntity.ok(response);
    }
}
