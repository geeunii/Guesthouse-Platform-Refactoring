package com.ssg9th2team.geharbang.domain.main.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation_theme.entity.AccommodationTheme;
import com.ssg9th2team.geharbang.domain.accommodation_theme.repository.AccommodationThemeRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import com.ssg9th2team.geharbang.domain.main.dto.MainAccommodationListResponse;
import com.ssg9th2team.geharbang.domain.main.repository.MainRepository;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository;
import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MainServiceTest {

    @InjectMocks
    private BaseMainService mainService;

    @Mock
    private MainRepository mainRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccommodationThemeRepository accommodationThemeRepository;
    @Mock
    private RoomJpaRepository roomJpaRepository;

    private User userWithThemes;
    private User userWithoutThemes;
    private Theme theme1, theme2, theme3;
    private Accommodation acc1, acc2, acc3;
    private List<Accommodation> allAccommodations;

    @BeforeEach
    void setUp() {
        // Create real entity objects but set IDs manually for testing
        theme1 = Theme.builder().themeCategory("여행스타일").themeName("오션뷰").build();
        ReflectionTestUtils.setField(theme1, "id", 1L);
        theme2 = Theme.builder().themeCategory("여행스타일").themeName("파티").build();
        ReflectionTestUtils.setField(theme2, "id", 2L);
        theme3 = Theme.builder().themeCategory("여행스타일").themeName("조용한").build();
        ReflectionTestUtils.setField(theme3, "id", 3L);

        userWithThemes = User.builder().themes(new HashSet<>(Set.of(theme1, theme2))).build();
        userWithoutThemes = User.builder().themes(new HashSet<>()).build();

        acc1 = Accommodation.builder()
                .accommodationsId(1L)
                .accommodationsName("오션뷰 파티하우스")
                .city("부산")
                .district("해운대")
                .township("우동")
                .rating(4.8)
                .reviewCount(150)
                .build();
        acc2 = Accommodation.builder()
                .accommodationsId(2L)
                .accommodationsName("조용한 오션뷰 숙소")
                .city("서울")
                .district("마포")
                .township("연남")
                .rating(4.5)
                .reviewCount(120)
                .build();
        acc3 = Accommodation.builder()
                .accommodationsId(3L)
                .accommodationsName("숲속의 조용한 집")
                .city("부산")
                .district("수영")
                .township("광안")
                .rating(4.9)
                .reviewCount(200)
                .build();
        allAccommodations = List.of(acc1, acc2, acc3);

        AccommodationTheme at1_1 = new AccommodationTheme(acc1, theme1);
        AccommodationTheme at1_2 = new AccommodationTheme(acc1, theme2);
        AccommodationTheme at2_1 = new AccommodationTheme(acc2, theme1);
        AccommodationTheme at2_3 = new AccommodationTheme(acc2, theme3);
        AccommodationTheme at3_3 = new AccommodationTheme(acc3, theme3);

        lenient().when(mainRepository.findByAccommodationStatusAndApprovalStatus(1, ApprovalStatus.APPROVED)).thenReturn(allAccommodations);
        lenient().when(mainRepository.findApprovedByKeyword("오션뷰")).thenReturn(List.of(acc1, acc2));
        lenient().when(mainRepository.findApprovedByKeyword("부산")).thenReturn(List.of(acc1, acc3));
        lenient().when(mainRepository.findApprovedByKeyword("파티")).thenReturn(List.of(acc1));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(userWithThemes));
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(userWithoutThemes));
        lenient().when(accommodationThemeRepository.findByAccommodationIds(anyList())).thenReturn(List.of(at1_1, at1_2, at2_1, at2_3, at3_3));
        lenient().when(mainRepository.findRepresentativeImages(anyList())).thenReturn(Collections.emptyList());
        lenient().when(roomJpaRepository.findMaxGuestsByAccommodationIds(anyList())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("로그인한 사용자(테마 선택O)에게 맞춤 숙소를 추천한다")
    void testRecommendationForUserWithThemes() {
        MainAccommodationListResponse response = mainService.getMainAccommodationList(1L, null, null);
        assertThat(response.getRecommendedAccommodations()).hasSize(2);
        assertThat(response.getRecommendedAccommodations().get(0).getAccommodationsName()).isEqualTo("오션뷰 파티하우스");
        assertThat(response.getRecommendedAccommodations().get(1).getAccommodationsName()).isEqualTo("조용한 오션뷰 숙소");
        assertThat(response.getGeneralAccommodations()).hasSize(1);
        assertThat(response.getGeneralAccommodations().get(0).getAccommodationsName()).isEqualTo("숲속의 조용한 집");
    }

    @Test
    @DisplayName("로그인한 사용자(테마 선택X)에게는 추천 숙소가 없다")
    void testRecommendationForUserWithoutThemes() {
        MainAccommodationListResponse response = mainService.getMainAccommodationList(2L, null, null);
        assertThat(response.getRecommendedAccommodations()).isEmpty();
        assertThat(response.getGeneralAccommodations()).hasSize(3);
    }

    @Test
    @DisplayName("비로그인 사용자에게는 추천 숙소가 없다")
    void testRecommendationForUnauthenticatedUser() {
        MainAccommodationListResponse response = mainService.getMainAccommodationList(null, null, null);
        assertThat(response.getRecommendedAccommodations()).isEmpty();
        assertThat(response.getGeneralAccommodations()).hasSize(3);
    }

    @Test
    @DisplayName("테마 필터 사용 시 추천 로직은 무시되고 필터링된 결과만 반환된다")
    void testFilteringWithThemeIds() {
        when(mainRepository.findByThemeIds(anyList())).thenReturn(List.of(acc2, acc3));

        MainAccommodationListResponse response = mainService.getMainAccommodationList(1L, List.of(3L), null);

        assertThat(response.getRecommendedAccommodations()).isEmpty();
        assertThat(response.getGeneralAccommodations()).hasSize(2);
        List<String> names = response.getGeneralAccommodations().stream().map(ListDto::getAccommodationsName).collect(Collectors.toList());
        assertThat(names).containsExactlyInAnyOrder("조용한 오션뷰 숙소", "숲속의 조용한 집");
    }

    @Test
    @DisplayName("검색어가 있으면 숙소명 기준으로 필터링된다")
    void testFilteringWithKeyword() {
        MainAccommodationListResponse response = mainService.getMainAccommodationList(2L, null, "오션뷰");

        assertThat(response.getRecommendedAccommodations()).isEmpty();
        List<String> names = response.getGeneralAccommodations().stream().map(ListDto::getAccommodationsName).collect(Collectors.toList());
        assertThat(names).containsExactlyInAnyOrder("오션뷰 파티하우스", "조용한 오션뷰 숙소");
    }

    @Test
    @DisplayName("검색어가 있으면 지역명으로도 필터링된다")
    void testFilteringWithKeywordMatchesLocation() {
        MainAccommodationListResponse response = mainService.getMainAccommodationList(2L, null, "부산");

        assertThat(response.getRecommendedAccommodations()).isEmpty();
        List<String> names = response.getGeneralAccommodations().stream().map(ListDto::getAccommodationsName).collect(Collectors.toList());
        assertThat(names).containsExactlyInAnyOrder("오션뷰 파티하우스", "숲속의 조용한 집");
    }

    @Test
    @DisplayName("검색어와 테마 필터를 함께 사용하면 교집합만 반환된다")
    void testFilteringWithKeywordAndThemeIds() {
        when(mainRepository.findByThemeIdsAndKeyword(eq(List.of(3L)), eq("오션뷰"))).thenReturn(List.of(acc2));

        MainAccommodationListResponse response = mainService.getMainAccommodationList(1L, List.of(3L), "오션뷰");

        assertThat(response.getRecommendedAccommodations()).isEmpty();
        assertThat(response.getGeneralAccommodations()).hasSize(1);
        assertThat(response.getGeneralAccommodations().get(0).getAccommodationsName()).isEqualTo("조용한 오션뷰 숙소");
    }

    @Test
    @DisplayName("검색어가 있으면 추천 숙소도 필터링된다")
    void testFilteringWithKeywordForRecommendations() {
        MainAccommodationListResponse response = mainService.getMainAccommodationList(1L, null, "파티");

        assertThat(response.getRecommendedAccommodations()).hasSize(1);
        assertThat(response.getRecommendedAccommodations().get(0).getAccommodationsName()).isEqualTo("오션뷰 파티하우스");
        assertThat(response.getGeneralAccommodations()).isEmpty();
    }
}
