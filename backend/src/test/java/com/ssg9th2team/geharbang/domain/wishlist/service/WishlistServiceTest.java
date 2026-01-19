package com.ssg9th2team.geharbang.domain.wishlist.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationCreateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.accommodation.service.AccommodationServiceImpl;
import com.ssg9th2team.geharbang.domain.wishlist.repository.jpa.WishlistJpaRepository;
import com.ssg9th2team.geharbang.global.storage.ObjectStorageService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("integration-test")
@Transactional
@Sql(scripts = "/sql/test-base-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class WishlistServiceTest extends com.ssg9th2team.geharbang.config.IntegrationTestConfig {

    @Autowired
    private WishlistServiceImpl wishlistService;

    @Autowired
    private AccommodationServiceImpl accommodationService;

    @MockBean
    private ObjectStorageService objectStorageService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private WishlistJpaRepository wishlistJpaRepository;

    private Long testUserId;
    private Long testAccommodationId;
    private Long testAccommodationId2;

    @BeforeEach
    void setUp() {
        // ObjectStorageService Mock 설정
        Mockito.when(objectStorageService.uploadBase64Image(Mockito.anyString(), Mockito.anyString()))
                .thenReturn("https://test-storage.com/test-image.jpg");

        testUserId = 1L;

        // 테스트용 숙소 1 생성
        testAccommodationId = createTestAccommodation("위시리스트 테스트 숙소 1");
        // 테스트용 숙소 2 생성
        testAccommodationId2 = createTestAccommodation("위시리스트 테스트 숙소 2");
    }

    private Long createTestAccommodation(String name) {
        AccommodationCreateRequestDto requestDto = new AccommodationCreateRequestDto();
        requestDto.setAccommodationsName(name);
        requestDto.setAccommodationsCategory("GUESTHOUSE");
        requestDto.setAccommodationsDescription("위시리스트 테스트용 숙소입니다");
        requestDto.setShortDescription("테스트 숙소");
        requestDto.setCity("부산");
        requestDto.setDistrict("해운대구");
        requestDto.setTownship("우동");
        requestDto.setAddressDetail("테스트 주소 456");
        requestDto.setLatitude(new BigDecimal("35.1595"));
        requestDto.setLongitude(new BigDecimal("129.1603"));
        requestDto.setTransportInfo("지하철 2호선 해운대역");
        requestDto.setPhone("051-1234-5678");
        requestDto.setBusinessRegistrationNumber("987-65-43210");
        requestDto.setBusinessRegistrationImage("test-business-image.jpg");
        requestDto.setParkingInfo("무료 주차");
        requestDto.setCheckInTime("14:00");
        requestDto.setCheckOutTime("11:00");
        requestDto.setBankName("부산은행");
        requestDto.setAccountNumber("111-222-333");
        requestDto.setAccountHolder("테스트");
        requestDto.setAmenityIds(new ArrayList<>());
        requestDto.setThemeIds(new ArrayList<>());
        requestDto.setRooms(new ArrayList<>());

        return accommodationService.createAccommodation(testUserId, requestDto);
    }

    @Test
    @DisplayName("위시리스트 추가 테스트")
    void addWishlistTest() {
        // given
        Long userId = 2L; // 다른 유저로 테스트

        // when
        wishlistService.addWishlist(userId, testAccommodationId);

        // then
        List<Long> wishlistIds = wishlistService.getMyWishlistAccommodationIds(userId);
        assertThat(wishlistIds).contains(testAccommodationId);

        System.out.println("위시리스트 추가 성공 - User: " + userId + ", Accommodation: " + testAccommodationId);
    }

    @Test
    @DisplayName("위시리스트 중복 추가 시 예외 발생 테스트")
    void addDuplicateWishlistTest() {
        // given
        Long userId = 3L;
        wishlistService.addWishlist(userId, testAccommodationId);

        // when & then
        assertThatThrownBy(() -> wishlistService.addWishlist(userId, testAccommodationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        System.out.println("중복 위시리스트 예외 처리 성공");
    }

    @Test
    @DisplayName("위시리스트 삭제 테스트")
    void removeWishlistTest() {
        // given
        Long userId = 4L;
        wishlistService.addWishlist(userId, testAccommodationId);
        entityManager.flush();

        // 추가 확인 (JPA로)
        assertThat(wishlistJpaRepository.existsByUserIdAndAccommodationsId(userId, testAccommodationId)).isTrue();

        // when
        wishlistService.removeWishlist(userId, testAccommodationId);

        // then (JPA로 검증)
        assertThat(wishlistJpaRepository.existsByUserIdAndAccommodationsId(userId, testAccommodationId)).isFalse();

        System.out.println("위시리스트 삭제 성공");
    }

    @Test
    @DisplayName("위시리스트 ID 목록 조회 테스트 (메인페이지용)")
    void getWishlistAccommodationIdsTest() {
        // given
        Long userId = 5L;
        wishlistService.addWishlist(userId, testAccommodationId);
        wishlistService.addWishlist(userId, testAccommodationId2);

        // when
        List<Long> wishlistIds = wishlistService.getMyWishlistAccommodationIds(userId);

        // then
        assertThat(wishlistIds).hasSize(2);
        assertThat(wishlistIds).contains(testAccommodationId, testAccommodationId2);

        System.out.println("위시리스트 ID 목록 조회 성공 - 총 " + wishlistIds.size() + "개");
    }

    @Test
    @DisplayName("위시리스트 상세 목록 조회 테스트 (마이페이지용)")
    void getMyWishlistDetailTest() {
        // given
        Long userId = 6L;
        wishlistService.addWishlist(userId, testAccommodationId);

        // when
        List<AccommodationResponseDto> wishlist = wishlistService.getMyWishlist(userId);

        // then
        assertThat(wishlist).isNotEmpty();
        assertThat(wishlist.get(0).getAccommodationsId()).isEqualTo(testAccommodationId);

        System.out.println("위시리스트 상세 조회 성공 - 숙소명: " + wishlist.get(0).getAccommodationsName());
    }

    @Test
    @DisplayName("빈 위시리스트 조회 테스트")
    void getEmptyWishlistTest() {
        // given
        Long userId = 999L; // 위시리스트가 없는 유저

        // when
        List<Long> wishlistIds = wishlistService.getMyWishlistAccommodationIds(userId);
        List<AccommodationResponseDto> wishlist = wishlistService.getMyWishlist(userId);

        // then
        assertThat(wishlistIds).isEmpty();
        assertThat(wishlist).isEmpty();

        System.out.println("빈 위시리스트 조회 성공");
    }

    @Test
    @DisplayName("위시리스트 추가 후 삭제 후 재추가 테스트")
    void addRemoveAddWishlistTest() {
        // given
        Long userId = 7L;

        // 1. 추가
        wishlistService.addWishlist(userId, testAccommodationId);
        entityManager.flush();
        assertThat(wishlistJpaRepository.existsByUserIdAndAccommodationsId(userId, testAccommodationId)).isTrue();

        // 2. 삭제
        wishlistService.removeWishlist(userId, testAccommodationId);
        assertThat(wishlistJpaRepository.existsByUserIdAndAccommodationsId(userId, testAccommodationId)).isFalse();

        // 3. 재추가 (예외 없이 성공해야 함)
        wishlistService.addWishlist(userId, testAccommodationId);
        entityManager.flush();
        assertThat(wishlistJpaRepository.existsByUserIdAndAccommodationsId(userId, testAccommodationId)).isTrue();

        System.out.println("위시리스트 추가/삭제/재추가 테스트 성공");
    }
}
