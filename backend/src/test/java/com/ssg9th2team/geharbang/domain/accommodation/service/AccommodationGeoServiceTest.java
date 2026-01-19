package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.admin.dto.GeoBackfillResponse;
import com.ssg9th2team.geharbang.domain.geocoding.GeoPoint;
import com.ssg9th2team.geharbang.domain.geocoding.GeocodingClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccommodationGeoServiceTest {

    @Mock
    private AccommodationJpaRepository accommodationRepository;

    @Mock
    private GeocodingClient geocodingClient;

    @InjectMocks
    private AccommodationGeoService accommodationGeoService;

    @Test
    void backfillMissingCoordinatesUpdatesWhenGeocodeFound() {
        Accommodation acc1 = Accommodation.builder()
                .accommodationsId(1L)
                .city("Seoul")
                .district("Gangnam")
                .township("Yeoksam")
                .addressDetail("123-1")
                .build();
        Accommodation acc2 = Accommodation.builder()
                .accommodationsId(2L)
                .build();

        when(accommodationRepository.findMissingCoordinates(any(Pageable.class)))
                .thenReturn(List.of(acc1, acc2));
        when(geocodingClient.geocode("Seoul Gangnam Yeoksam 123-1"))
                .thenReturn(Optional.of(new GeoPoint(new BigDecimal("37.5"), new BigDecimal("127.0"))));
        when(accommodationRepository.countByLatitudeIsNullOrLongitudeIsNull())
                .thenReturn(5L);

        GeoBackfillResponse response = accommodationGeoService.backfillMissingCoordinates(10);

        assertThat(response.processed()).isEqualTo(2);
        assertThat(response.updated()).isEqualTo(1);
        assertThat(response.skipped()).isEqualTo(1);
        assertThat(response.failed()).isEqualTo(0);
        assertThat(response.remaining()).isEqualTo(5L);
        assertThat(acc1.getLatitude()).isEqualByComparingTo("37.5");
        assertThat(acc1.getLongitude()).isEqualByComparingTo("127.0");

        verify(accommodationRepository).flush();
    }
}
