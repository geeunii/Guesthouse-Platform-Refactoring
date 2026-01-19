package com.ssg9th2team.geharbang.domain.recommendation.repository;

import com.ssg9th2team.geharbang.domain.recommendation.dto.AccommodationScoreDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface RecommendationMapper {

        /**
         * 사용자 선호 테마 ID 목록 조회
         */
        Set<Long> findUserThemeIds(@Param("userId") Long userId);

        /**
         * 사용자가 높은 평점(4점 이상) 준 리뷰의 태그 ID 목록 조회
         */
        Set<Long> findPreferredTagIds(@Param("userId") Long userId);

        /**
         * 테마 매칭 기반 숙소 조회
         */
        List<AccommodationScoreDto> findByThemeMatch(
                        @Param("themeIds") Set<Long> themeIds,
                        @Param("excludeIds") Set<Long> excludeIds,
                        @Param("limit") int limit);

        /**
         * 태그 매칭 기반 숙소 조회
         */
        List<AccommodationScoreDto> findByTagMatch(
                        @Param("tagIds") Set<Long> tagIds,
                        @Param("excludeIds") Set<Long> excludeIds,
                        @Param("limit") int limit);

        /**
         * 사용자가 이미 예약한 숙소 ID 목록
         */
        Set<Long> findReservedAccommodationIds(@Param("userId") Long userId);

        /**
         * 테마명 조회
         */
        List<String> findThemeNamesByAccommodationId(@Param("accommodationId") Long accommodationId);

        /**
         * 숙소의 인기 태그 조회
         */
        List<String> findTopTagNamesByAccommodationId(@Param("accommodationId") Long accommodationId);

        /**
         * [Fallback] 인기 숙소 추천 (선호 테마/태그 없는 신규 사용자용)
         * - 예약 수 + 평점 기반 정렬
         */
        List<AccommodationScoreDto> findPopularAccommodations(
                        @Param("excludeIds") Set<Long> excludeIds,
                        @Param("limit") int limit);
}
