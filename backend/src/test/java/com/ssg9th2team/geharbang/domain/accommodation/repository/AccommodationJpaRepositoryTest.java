package com.ssg9th2team.geharbang.domain.accommodation.repository;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.global.storage.ObjectStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AccommodationJpaRepositoryTest {

    @Autowired
    private AccommodationJpaRepository accommodationJpaRepository;

    @MockBean
    private ObjectStorageService objectStorageService;

    @Test
    @DisplayName("DB에서 숙소 1개 조회 테스트")
    void findOneAccommodation() {
        // given - DB에 데이터가 있다고 가정

        // when - 첫 번째 숙소 조회
        Optional<Accommodation> result = accommodationJpaRepository.findAll()
                .stream()
                .findFirst();

        // then - 결과 출력 및 검증
        if (result.isPresent()) {
            Accommodation accommodation = result.get();
            System.out.println("=== 조회된 숙소 정보 ===");
            System.out.println("ID: " + accommodation.getAccommodationsId());
            System.out.println("이름: " + accommodation.getAccommodationsName());
            System.out.println("도시: " + accommodation.getCity());
            System.out.println("설명: " + accommodation.getAccommodationsDescription());

            assertThat(accommodation.getAccommodationsId()).isNotNull();
        } else {
            System.out.println("DB에 숙소 데이터가 없습니다.");
        }
    }
}
