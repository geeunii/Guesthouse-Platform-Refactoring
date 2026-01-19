package com.ssg9th2team.geharbang.domain.search.service;

import com.ssg9th2team.geharbang.domain.main.dto.PublicListResponse;
import com.ssg9th2team.geharbang.domain.main.repository.ListDtoProjection;
import com.ssg9th2team.geharbang.domain.search.repository.SearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @InjectMocks
    private SearchServiceImpl searchService;

    @Mock
    private SearchRepository searchRepository;

    @Test
    @DisplayName("공개 검색은 페이지 응답을 매핑한다")
    void testSearchPublicListMapsPage() {
        ListDtoProjection projection = new ListProjectionStub(
                10L,
                "오션뷰 숙소",
                "설명",
                "부산",
                "해운대",
                "좌동",
                35.1,
                129.1,
                120000L,
                4.7,
                12,
                4,
                "https://example.com/image.jpg"
        );
        PageImpl<ListDtoProjection> page = new PageImpl<>(List.of(projection), PageRequest.of(0, 24), 1);
        when(searchRepository.searchPublicListNoDates(eq("부산"), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        PublicListResponse response = searchService.searchPublicList(
                Collections.emptyList(),
                " 부산 ",
                0,
                24,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).getAccommodationsName()).isEqualTo("오션뷰 숙소");
        assertThat(response.page().totalElements()).isEqualTo(1);
        assertThat(response.page().hasNext()).isFalse();
    }

    @Test
    @DisplayName("테마 검색은 테마용 쿼리를 사용한다")
    void testSearchPublicListUsesThemeQuery() {
        ListDtoProjection projection = new ListProjectionStub(
                11L,
                "테마 숙소",
                "설명",
                "서울",
                "마포",
                "연남",
                37.5,
                126.9,
                90000L,
                4.2,
                5,
                3,
                "https://example.com/theme.jpg"
        );
        PageImpl<ListDtoProjection> page = new PageImpl<>(List.of(projection), PageRequest.of(1, 10), 11);
        when(searchRepository.searchPublicListByThemeNoDates(eq(List.of(2L)), eq("오션뷰"), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        PublicListResponse response = searchService.searchPublicList(
                List.of(2L),
                "오션뷰",
                1,
                10,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(response.items()).hasSize(1);
        verify(searchRepository).searchPublicListByThemeNoDates(eq(List.of(2L)), eq("오션뷰"), isNull(), any(PageRequest.class));
    }

    @Test
    @DisplayName("지도 검색은 좌표 조건 쿼리를 사용한다")
    void testSearchPublicListUsesBoundsQuery() {
        ListDtoProjection projection = new ListProjectionStub(
                12L,
                "지도 숙소",
                "설명",
                "부산",
                "해운대",
                "좌동",
                35.2,
                129.1,
                130000L,
                4.1,
                8,
                2,
                "https://example.com/map.jpg"
        );
        PageImpl<ListDtoProjection> page = new PageImpl<>(List.of(projection), PageRequest.of(0, 50), 1);
        when(searchRepository.searchPublicListByBoundsNoDates(
                eq("부산"),
                eq(35.0),
                eq(36.0),
                eq(126.0),
                eq(128.0),
                isNull(),
                any(PageRequest.class)
        )).thenReturn(page);

        PublicListResponse response = searchService.searchPublicList(
                Collections.emptyList(),
                "부산",
                0,
                50,
                35.0,
                36.0,
                126.0,
                128.0,
                null,
                null,
                null
        );

        assertThat(response.items()).hasSize(1);
        verify(searchRepository).searchPublicListByBoundsNoDates(
                eq("부산"),
                eq(35.0),
                eq(36.0),
                eq(126.0),
                eq(128.0),
                isNull(),
                any(PageRequest.class)
        );
    }

    @Test
    @DisplayName("날짜/인원 조건이 있으면 검색 쿼리에 전달된다")
    void testSearchPublicListPassesAvailabilityParams() {
        ListDtoProjection projection = new ListProjectionStub(
                20L,
                "예약 가능 숙소",
                "설명",
                "서울",
                "강남",
                "역삼",
                37.5,
                127.0,
                150000L,
                4.6,
                9,
                4,
                "https://example.com/available.jpg"
        );
        PageImpl<ListDtoProjection> page = new PageImpl<>(List.of(projection), PageRequest.of(0, 10), 1);
        LocalDateTime checkin = LocalDateTime.of(2026, 1, 10, 15, 0);
        LocalDateTime checkout = LocalDateTime.of(2026, 1, 12, 11, 0);
        when(searchRepository.searchPublicList(isNull(), eq(checkin), eq(checkout), eq(4), any(PageRequest.class)))
                .thenReturn(page);

        PublicListResponse response = searchService.searchPublicList(
                Collections.emptyList(),
                null,
                0,
                10,
                null,
                null,
                null,
                null,
                checkin,
                checkout,
                4
        );

        assertThat(response.items()).hasSize(1);
        verify(searchRepository).searchPublicList(isNull(), eq(checkin), eq(checkout), eq(4), any(PageRequest.class));
    }

    private static final class ListProjectionStub implements ListDtoProjection {
        private final Long accommodationsId;
        private final String accommodationsName;
        private final String shortDescription;
        private final String city;
        private final String district;
        private final String township;
        private final Double latitude;
        private final Double longitude;
        private final Long minPrice;
        private final Double rating;
        private final Integer reviewCount;
        private final Integer maxGuests;
        private final String imageUrl;

        private ListProjectionStub(Long accommodationsId, String accommodationsName, String shortDescription, String city,
                                   String district, String township, Double latitude, Double longitude, Long minPrice,
                                   Double rating, Integer reviewCount, Integer maxGuests, String imageUrl) {
            this.accommodationsId = accommodationsId;
            this.accommodationsName = accommodationsName;
            this.shortDescription = shortDescription;
            this.city = city;
            this.district = district;
            this.township = township;
            this.latitude = latitude;
            this.longitude = longitude;
            this.minPrice = minPrice;
            this.rating = rating;
            this.reviewCount = reviewCount;
            this.maxGuests = maxGuests;
            this.imageUrl = imageUrl;
        }

        @Override
        public Long getAccommodationsId() {
            return accommodationsId;
        }

        @Override
        public String getAccommodationsName() {
            return accommodationsName;
        }

        @Override
        public String getShortDescription() {
            return shortDescription;
        }

        @Override
        public String getCity() {
            return city;
        }

        @Override
        public String getDistrict() {
            return district;
        }

        @Override
        public String getTownship() {
            return township;
        }

        @Override
        public Double getLatitude() {
            return latitude;
        }

        @Override
        public Double getLongitude() {
            return longitude;
        }

        @Override
        public Long getMinPrice() {
            return minPrice;
        }

        @Override
        public Double getRating() {
            return rating;
        }

        @Override
        public Integer getReviewCount() {
            return reviewCount;
        }

        @Override
        public Integer getMaxGuests() {
            return maxGuests;
        }

        @Override
        public String getImageUrl() {
            return imageUrl;
        }
    }
}
