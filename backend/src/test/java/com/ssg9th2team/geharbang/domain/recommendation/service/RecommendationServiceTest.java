package com.ssg9th2team.geharbang.domain.recommendation.service;

import com.ssg9th2team.geharbang.domain.recommendation.dto.AccommodationScoreDto;
import com.ssg9th2team.geharbang.domain.recommendation.dto.RecommendationResponse;
import com.ssg9th2team.geharbang.domain.recommendation.repository.RecommendationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 추천 시스템 단위 테스트 (MockBean 사용)
 * 
 * 테스트 시나리오:
 * 1. 신규 유저 (선호 테마 없음, 리뷰 없음) → Fallback 인기 숙소 추천
 * 2. 테마만 선택한 유저 → 테마 기반 추천
 * 3. 리뷰만 작성한 유저 (4점 이상) → 태그 기반 추천
 * 4. 테마 + 리뷰 모두 있는 유저 → 하이브리드 추천
 */
@ExtendWith(MockitoExtension.class)
public class RecommendationServiceTest {

    @Mock
    private RecommendationMapper recommendationMapper;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private AccommodationScoreDto mockAccommodation1;
    private AccommodationScoreDto mockAccommodation2;
    private AccommodationScoreDto mockAccommodation3;

    @BeforeEach
    void setUp() {
        // Mock 숙소 데이터 생성
        mockAccommodation1 = AccommodationScoreDto.builder()
                .accommodationId(1L)
                .accommodationName("해변 게스트하우스")
                .shortDescription("바다가 보이는 아늑한 숙소")
                .city("부산")
                .district("해운대구")
                .imageUrl("https://example.com/beach.jpg")
                .rating(4.5)
                .minPrice(35000.0)
                .themeMatchCount(2)
                .tagMatchCount(0)
                .reviewCount(10)
                .totalThemeCount(2)
                .reservationCount(25)
                .build();

        mockAccommodation2 = AccommodationScoreDto.builder()
                .accommodationId(2L)
                .accommodationName("조용한 산장")
                .shortDescription("힐링이 되는 산속 숙소")
                .city("강원")
                .district("평창군")
                .imageUrl("https://example.com/mountain.jpg")
                .rating(4.2)
                .minPrice(45000.0)
                .themeMatchCount(1)
                .tagMatchCount(3)
                .reviewCount(5)
                .totalThemeCount(1)
                .reservationCount(15)
                .build();

        mockAccommodation3 = AccommodationScoreDto.builder()
                .accommodationId(3L)
                .accommodationName("도심 호스텔")
                .shortDescription("편리한 위치의 가성비 숙소")
                .city("서울")
                .district("마포구")
                .imageUrl("https://example.com/city.jpg")
                .rating(4.0)
                .minPrice(20000.0)
                .themeMatchCount(0)
                .tagMatchCount(2)
                .reviewCount(20)
                .totalThemeCount(1)
                .reservationCount(50)
                .build();
    }

    @Test
    @DisplayName("신규 유저 - Fallback 인기 숙소 추천")
    void newUserFallbackRecommendationTest() {
        // given
        Long newUserId = 9999L;

        // 신규 유저: 테마/태그 없음
        when(recommendationMapper.findUserThemeIds(newUserId)).thenReturn(new HashSet<>());
        when(recommendationMapper.findPreferredTagIds(newUserId)).thenReturn(new HashSet<>());
        when(recommendationMapper.findReservedAccommodationIds(newUserId)).thenReturn(new HashSet<>());

        // Fallback: 인기 숙소 반환
        when(recommendationMapper.findPopularAccommodations(anySet(), anyInt()))
                .thenReturn(List.of(mockAccommodation3, mockAccommodation1, mockAccommodation2));

        // 테마/태그 이름 반환
        when(recommendationMapper.findThemeNamesByAccommodationId(anyLong()))
                .thenReturn(List.of("도심", "가성비"));
        when(recommendationMapper.findTopTagNamesByAccommodationId(anyLong()))
                .thenReturn(List.of("위치 좋음", "친절함"));

        // when
        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(newUserId, 5);

        // then
        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations.size()).isLessThanOrEqualTo(5);

        // 모든 추천에 필수 필드가 있어야 함
        recommendations.forEach(rec -> {
            assertThat(rec.getAccommodationId()).isNotNull();
            assertThat(rec.getAccommodationName()).isNotNull();
            assertThat(rec.getScore()).isNotNull();
        });

        System.out.println("=== 신규 유저 Fallback 추천 테스트 통과 ===");
        System.out.println("추천 수: " + recommendations.size());
    }

    @Test
    @DisplayName("테마만 선택한 유저 - 테마 기반 추천")
    void userWithOnlyThemePreferenceTest() {
        // given
        Long userId = 101L;
        Set<Long> userThemes = Set.of(1L, 2L); // 바다뷰, 조용한

        when(recommendationMapper.findUserThemeIds(userId)).thenReturn(userThemes);
        when(recommendationMapper.findPreferredTagIds(userId)).thenReturn(new HashSet<>());
        when(recommendationMapper.findReservedAccommodationIds(userId)).thenReturn(new HashSet<>());

        // 테마 매칭 숙소 반환
        when(recommendationMapper.findByThemeMatch(eq(userThemes), anySet(), anyInt()))
                .thenReturn(List.of(mockAccommodation1, mockAccommodation2));

        // 테마/태그 이름 반환
        when(recommendationMapper.findThemeNamesByAccommodationId(1L))
                .thenReturn(List.of("바다뷰", "조용한"));
        when(recommendationMapper.findThemeNamesByAccommodationId(2L))
                .thenReturn(List.of("조용한"));
        when(recommendationMapper.findTopTagNamesByAccommodationId(anyLong()))
                .thenReturn(List.of());

        // when
        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(userId, 5);

        // then
        assertThat(recommendations).isNotEmpty();

        // 점수가 계산되어 있어야 함
        recommendations.forEach(rec -> {
            assertThat(rec.getScore()).isGreaterThan(0.0);
        });

        System.out.println("=== 테마만 선택한 유저 추천 테스트 통과 ===");
        System.out.println("추천 수: " + recommendations.size());
    }

    @Test
    @DisplayName("리뷰만 작성한 유저 - 태그 기반 추천")
    void userWithOnlyReviewTest() {
        // given
        Long userId = 102L;
        Set<Long> preferredTags = Set.of(1L, 3L); // 친절한 호스트, 위치 좋음

        when(recommendationMapper.findUserThemeIds(userId)).thenReturn(new HashSet<>());
        when(recommendationMapper.findPreferredTagIds(userId)).thenReturn(preferredTags);
        when(recommendationMapper.findReservedAccommodationIds(userId)).thenReturn(new HashSet<>());

        // 태그 매칭 숙소 반환
        when(recommendationMapper.findByTagMatch(eq(preferredTags), anySet(), anyInt()))
                .thenReturn(List.of(mockAccommodation2, mockAccommodation3));

        // 테마/태그 이름 반환
        when(recommendationMapper.findThemeNamesByAccommodationId(anyLong()))
                .thenReturn(List.of());
        when(recommendationMapper.findTopTagNamesByAccommodationId(2L))
                .thenReturn(List.of("친절한 호스트", "위치 좋음", "청결함"));
        when(recommendationMapper.findTopTagNamesByAccommodationId(3L))
                .thenReturn(List.of("위치 좋음", "가성비"));

        // when
        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(userId, 5);

        // then
        assertThat(recommendations).isNotEmpty();

        System.out.println("=== 리뷰만 작성한 유저 추천 테스트 통과 ===");
        System.out.println("추천 수: " + recommendations.size());
    }

    @Test
    @DisplayName("테마 + 리뷰 모두 있는 유저 - 하이브리드 추천")
    void userWithBothThemeAndReviewTest() {
        // given
        Long userId = 103L;
        Set<Long> userThemes = Set.of(1L);
        Set<Long> preferredTags = Set.of(1L, 2L);

        when(recommendationMapper.findUserThemeIds(userId)).thenReturn(userThemes);
        when(recommendationMapper.findPreferredTagIds(userId)).thenReturn(preferredTags);
        when(recommendationMapper.findReservedAccommodationIds(userId)).thenReturn(new HashSet<>());

        // 테마/태그 매칭 숙소 반환
        when(recommendationMapper.findByThemeMatch(eq(userThemes), anySet(), anyInt()))
                .thenReturn(List.of(mockAccommodation1));
        when(recommendationMapper.findByTagMatch(eq(preferredTags), anySet(), anyInt()))
                .thenReturn(List.of(mockAccommodation2, mockAccommodation3));

        // 테마/태그 이름 반환
        when(recommendationMapper.findThemeNamesByAccommodationId(anyLong()))
                .thenReturn(List.of("바다뷰"));
        when(recommendationMapper.findTopTagNamesByAccommodationId(anyLong()))
                .thenReturn(List.of("친절함", "청결함"));

        // when
        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(userId, 5);

        // then
        assertThat(recommendations).isNotEmpty();

        // 점수 순으로 정렬되어 있어야 함
        for (int i = 0; i < recommendations.size() - 1; i++) {
            assertThat(recommendations.get(i).getScore())
                    .isGreaterThanOrEqualTo(recommendations.get(i + 1).getScore());
        }

        System.out.println("=== 테마 + 리뷰 유저 하이브리드 추천 테스트 통과 ===");
        System.out.println("추천 수: " + recommendations.size());
    }

    @Test
    @DisplayName("이미 예약한 숙소는 추천에서 제외")
    void excludeAlreadyReservedAccommodationsTest() {
        // given
        Long userId = 103L;
        Set<Long> reservedIds = Set.of(1L, 2L); // 이미 예약한 숙소

        when(recommendationMapper.findUserThemeIds(userId)).thenReturn(new HashSet<>());
        when(recommendationMapper.findPreferredTagIds(userId)).thenReturn(new HashSet<>());
        when(recommendationMapper.findReservedAccommodationIds(userId)).thenReturn(reservedIds);

        // Fallback 사용 (테마/태그 없음)
        when(recommendationMapper.findPopularAccommodations(eq(reservedIds), anyInt()))
                .thenReturn(List.of(mockAccommodation3)); // 예약 안 한 숙소만 반환

        when(recommendationMapper.findThemeNamesByAccommodationId(anyLong()))
                .thenReturn(List.of());
        when(recommendationMapper.findTopTagNamesByAccommodationId(anyLong()))
                .thenReturn(List.of());

        // when
        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(userId, 5);

        // then
        assertThat(recommendations).isNotEmpty();

        // 예약한 숙소가 추천에 없어야 함
        recommendations.forEach(rec -> {
            assertThat(reservedIds).doesNotContain(rec.getAccommodationId());
        });

        System.out.println("=== 예약 숙소 제외 테스트 통과 ===");
    }

    @Test
    @DisplayName("추천 결과에 필수 정보가 포함되어 있는지 확인")
    void recommendationResponseContainsRequiredFieldsTest() {
        // given
        Long userId = 9999L;

        when(recommendationMapper.findUserThemeIds(userId)).thenReturn(new HashSet<>());
        when(recommendationMapper.findPreferredTagIds(userId)).thenReturn(new HashSet<>());
        when(recommendationMapper.findReservedAccommodationIds(userId)).thenReturn(new HashSet<>());
        when(recommendationMapper.findPopularAccommodations(anySet(), anyInt()))
                .thenReturn(List.of(mockAccommodation1));
        when(recommendationMapper.findThemeNamesByAccommodationId(anyLong()))
                .thenReturn(List.of("테마1"));
        when(recommendationMapper.findTopTagNamesByAccommodationId(anyLong()))
                .thenReturn(List.of("태그1"));

        // when
        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(userId, 3);

        // then
        assertThat(recommendations).isNotEmpty();
        recommendations.forEach(rec -> {
            assertThat(rec.getAccommodationId()).isNotNull();
            assertThat(rec.getAccommodationName()).isNotNull();
            assertThat(rec.getCity()).isNotNull();
            assertThat(rec.getScore()).isNotNull();
            assertThat(rec.getMatchedThemes()).isNotNull();
            assertThat(rec.getMatchedTags()).isNotNull();
        });

        System.out.println("=== 응답 필드 검증 테스트 통과 ===");
    }

    @Test
    @DisplayName("limit 파라미터가 정상 동작하는지 확인")
    void limitParameterTest() {
        // given
        Long userId = 9999L;
        int limit = 2;

        when(recommendationMapper.findUserThemeIds(userId)).thenReturn(new HashSet<>());
        when(recommendationMapper.findPreferredTagIds(userId)).thenReturn(new HashSet<>());
        when(recommendationMapper.findReservedAccommodationIds(userId)).thenReturn(new HashSet<>());
        when(recommendationMapper.findPopularAccommodations(anySet(), eq(limit)))
                .thenReturn(List.of(mockAccommodation1, mockAccommodation2));
        when(recommendationMapper.findThemeNamesByAccommodationId(anyLong()))
                .thenReturn(List.of());
        when(recommendationMapper.findTopTagNamesByAccommodationId(anyLong()))
                .thenReturn(List.of());

        // when
        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(userId, limit);

        // then
        assertThat(recommendations.size()).isLessThanOrEqualTo(limit);

        System.out.println("=== Limit 파라미터 테스트 통과 ===");
    }
}
