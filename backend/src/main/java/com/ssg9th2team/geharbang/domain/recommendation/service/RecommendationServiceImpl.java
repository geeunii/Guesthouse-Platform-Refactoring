package com.ssg9th2team.geharbang.domain.recommendation.service;

import com.ssg9th2team.geharbang.domain.recommendation.dto.AccommodationScoreDto;
import com.ssg9th2team.geharbang.domain.recommendation.dto.RecommendationResponse;
import com.ssg9th2team.geharbang.domain.recommendation.repository.RecommendationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationMapper recommendationMapper;

    // 가중치 설정
    private static final double THEME_WEIGHT = 0.4;
    private static final double TAG_WEIGHT = 0.35;
    private static final double RATING_WEIGHT = 0.25;

    @Override
    public List<RecommendationResponse> getRecommendations(Long userId, int limit) {
        // 1. 사용자 선호 테마 조회
        Set<Long> userThemeIds = recommendationMapper.findUserThemeIds(userId);
        if (userThemeIds == null)
            userThemeIds = new HashSet<>();

        // 2. 사용자가 높은 평점 준 태그 조회
        Set<Long> preferredTagIds = recommendationMapper.findPreferredTagIds(userId);
        if (preferredTagIds == null)
            preferredTagIds = new HashSet<>();

        // 3. 이미 예약한 숙소 제외
        Set<Long> reservedIds = recommendationMapper.findReservedAccommodationIds(userId);
        if (reservedIds == null)
            reservedIds = new HashSet<>();

        // [Fallback] 사용자 선호 테마/태그가 없으면 인기 숙소 추천
        if (userThemeIds.isEmpty() && preferredTagIds.isEmpty()) {
            log.info("User {} has no preferences, using popularity-based fallback", userId);
            return getPopularRecommendations(reservedIds, limit);
        }

        // 4. 테마 기반 숙소 조회
        List<AccommodationScoreDto> themeMatched = new ArrayList<>();
        if (!userThemeIds.isEmpty()) {
            themeMatched = recommendationMapper.findByThemeMatch(userThemeIds, reservedIds, limit * 2);
        }

        // 5. 태그 기반 숙소 조회
        List<AccommodationScoreDto> tagMatched = new ArrayList<>();
        if (!preferredTagIds.isEmpty()) {
            tagMatched = recommendationMapper.findByTagMatch(preferredTagIds, reservedIds, limit * 2);
        }

        // 6. 점수 계산 및 병합
        Map<Long, ScoreAccumulator> scoreMap = new HashMap<>();

        // 테마 매칭 점수 추가
        for (AccommodationScoreDto dto : themeMatched) {
            scoreMap.computeIfAbsent(dto.getAccommodationId(), k -> new ScoreAccumulator(dto))
                    .addThemeScore(dto.getThemeMatchCount());
        }

        // 태그 매칭 점수 추가
        for (AccommodationScoreDto dto : tagMatched) {
            scoreMap.computeIfAbsent(dto.getAccommodationId(), k -> new ScoreAccumulator(dto))
                    .addTagScore(dto.getTagMatchCount());
        }

        // 7. 최종 점수 계산 및 정렬
        final int userThemeCount = userThemeIds.size();
        List<RecommendationResponse> recommendations = scoreMap.values().stream()
                .map(acc -> toRecommendationResponse(acc, userThemeCount))
                .sorted(Comparator.comparing(RecommendationResponse::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // 8. 테마/태그 이름 추가
        for (RecommendationResponse rec : recommendations) {
            List<String> themeNames = recommendationMapper.findThemeNamesByAccommodationId(rec.getAccommodationId());
            List<String> tagNames = recommendationMapper.findTopTagNamesByAccommodationId(rec.getAccommodationId());

            // Builder 패턴으로 새 객체 생성 (immutable)
            RecommendationResponse updated = RecommendationResponse.builder()
                    .accommodationId(rec.getAccommodationId())
                    .accommodationName(rec.getAccommodationName())
                    .shortDescription(rec.getShortDescription())
                    .city(rec.getCity())
                    .district(rec.getDistrict())
                    .imageUrl(rec.getImageUrl())
                    .rating(rec.getRating())
                    .minPrice(rec.getMinPrice())
                    .score(rec.getScore())
                    .matchedThemes(themeNames != null ? themeNames : List.of())
                    .matchedTags(tagNames != null ? tagNames : List.of())
                    .build();

            // 리스트에서 교체
            int idx = recommendations.indexOf(rec);
            recommendations.set(idx, updated);
        }

        return recommendations;
    }

    private RecommendationResponse toRecommendationResponse(ScoreAccumulator acc, int userThemeCount) {
        // 1. 테마 점수: Jaccard Similarity 적용
        // 교집합(themeScore) / 합집합(userThemeCount + totalThemeCount - themeScore)
        double themeJaccard = 0.0;
        int totalThemeCount = acc.dto.getTotalThemeCount() != null ? acc.dto.getTotalThemeCount() : 0;
        int unionSize = userThemeCount + totalThemeCount - (int) acc.themeScore;

        if (unionSize > 0) {
            themeJaccard = acc.themeScore / (double) unionSize;
        }

        // 2. 태그 점수 (기존 유지: 최대 5개 매칭 시 1.0)
        double normalizedTag = Math.min(acc.tagScore / 5.0, 1.0);

        // 3. 평점 점수: Bayesian Average 적용
        // (v*R + C*m) / (v+C)
        // v: 리뷰수, R: 평점, C=5(가중치), m=4.0(평균)
        double rating = acc.dto.getRating() != null ? acc.dto.getRating() : 0.0;
        int reviewCount = acc.dto.getReviewCount() != null ? acc.dto.getReviewCount() : 0;
        double C = 5.0;
        double m = 4.0;
        double bayesianRating = (reviewCount * rating + C * m) / (reviewCount + C);
        double normalizedRating = bayesianRating / 5.0; // 5점 만점을 0~1로 정규화

        double finalScore = (themeJaccard * THEME_WEIGHT) +
                (normalizedTag * TAG_WEIGHT) +
                (normalizedRating * RATING_WEIGHT);

        return RecommendationResponse.builder()
                .accommodationId(acc.dto.getAccommodationId())
                .accommodationName(acc.dto.getAccommodationName())
                .shortDescription(acc.dto.getShortDescription())
                .city(acc.dto.getCity())
                .district(acc.dto.getDistrict())
                .imageUrl(acc.dto.getImageUrl())
                .rating(acc.dto.getRating())
                .minPrice(acc.dto.getMinPrice() != null ? acc.dto.getMinPrice().intValue() : 0)
                .score(Math.round(finalScore * 100) / 100.0) // 100점 만점이 아니라 1.0 만점 기준이면 곱하기 100 필요. 기존 로직 유지
                .matchedThemes(List.of())
                .matchedTags(List.of())
                .build();
    }

    /**
     * [Fallback] 인기 숙소 기반 추천
     * 선호 테마/태그가 없는 신규 사용자를 위한 추천
     */
    private List<RecommendationResponse> getPopularRecommendations(Set<Long> excludeIds, int limit) {
        List<AccommodationScoreDto> popularAccommodations = recommendationMapper.findPopularAccommodations(excludeIds,
                limit);

        List<RecommendationResponse> recommendations = new ArrayList<>();
        for (AccommodationScoreDto dto : popularAccommodations) {
            // 인기도 점수 계산 (예약 수 + 평점 기반)
            double popularityScore = calculatePopularityScore(dto);

            List<String> themeNames = recommendationMapper.findThemeNamesByAccommodationId(dto.getAccommodationId());
            List<String> tagNames = recommendationMapper.findTopTagNamesByAccommodationId(dto.getAccommodationId());

            RecommendationResponse response = RecommendationResponse.builder()
                    .accommodationId(dto.getAccommodationId())
                    .accommodationName(dto.getAccommodationName())
                    .shortDescription(dto.getShortDescription())
                    .city(dto.getCity())
                    .district(dto.getDistrict())
                    .imageUrl(dto.getImageUrl())
                    .rating(dto.getRating())
                    .minPrice(dto.getMinPrice() != null ? dto.getMinPrice().intValue() : 0)
                    .score(Math.round(popularityScore * 100) / 100.0)
                    .matchedThemes(themeNames != null ? themeNames : List.of())
                    .matchedTags(tagNames != null ? tagNames : List.of())
                    .build();

            recommendations.add(response);
        }

        return recommendations;
    }

    /**
     * 인기도 점수 계산 (Fallback용)
     * - 예약 수 정규화 (최대 50건 기준)
     * - 베이지안 평점
     */
    private double calculatePopularityScore(AccommodationScoreDto dto) {
        // 예약 수 정규화 (50건 이상이면 1.0)
        int reservationCount = dto.getReservationCount() != null ? dto.getReservationCount() : 0;
        double normalizedReservations = Math.min(reservationCount / 50.0, 1.0);

        // 베이지안 평점
        double rating = dto.getRating() != null ? dto.getRating() : 0.0;
        int reviewCount = dto.getReviewCount() != null ? dto.getReviewCount() : 0;
        double bayesianRating = (reviewCount * rating + 5.0 * 4.0) / (reviewCount + 5.0);
        double normalizedRating = bayesianRating / 5.0;

        // 예약 수 50% + 평점 50%
        return (normalizedReservations * 0.5) + (normalizedRating * 0.5);
    }

    // 점수 누적용 내부 클래스
    private static class ScoreAccumulator {
        AccommodationScoreDto dto;
        double themeScore = 0;
        double tagScore = 0;

        ScoreAccumulator(AccommodationScoreDto dto) {
            this.dto = dto;
        }

        void addThemeScore(int count) {
            this.themeScore += count;
        }

        void addTagScore(int count) {
            this.tagScore += count;
        }
    }
}
