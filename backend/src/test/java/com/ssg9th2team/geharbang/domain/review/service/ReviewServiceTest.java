package com.ssg9th2team.geharbang.domain.review.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationCreateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.service.AccommodationServiceImpl;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewCreateDto;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.review.repository.mybatis.ReviewMapper;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;

@ActiveProfiles("integration-test")
@Transactional
@Sql(scripts = "/sql/test-base-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class ReviewServiceTest extends com.ssg9th2team.geharbang.config.IntegrationTestConfig {

    @Autowired
    private ReviewServiceImpl reviewService;

    @Autowired
    private AccommodationServiceImpl accommodationService;

    @Autowired
    private ReviewJpaRepository reviewJpaRepository;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private ObjectStorageService objectStorageService;

    @MockBean
    private ReviewMapper reviewMapper;

    @MockBean
    private com.ssg9th2team.geharbang.domain.coupon.service.UserCouponService userCouponService;

    private Long testUserId;
    private Long testAccommodationId;

    @BeforeEach
    void setUp() {
        // ObjectStorageService Mock 설정
        Mockito.when(objectStorageService.uploadBase64Image(anyString(), anyString()))
                .thenReturn("https://test-storage.com/test-image.jpg");

        // ReviewMapper Mock 설정 (태그 저장)
        Mockito.doNothing().when(reviewMapper).insertReviewTags(anyLong(), anyList());

        // UserCouponService Mock 설정
        Mockito.doNothing().when(userCouponService).issueReviewRewardCoupon(anyLong());

        testUserId = 1L;  // HOST 유저
        testAccommodationId = createTestAccommodation("리뷰 테스트 숙소");
    }

    /**
     * 테스트용 숙소 생성
     */
    private Long createTestAccommodation(String name) {
        AccommodationCreateRequestDto requestDto = new AccommodationCreateRequestDto();
        requestDto.setAccommodationsName(name);
        requestDto.setAccommodationsCategory("GUESTHOUSE");
        requestDto.setAccommodationsDescription("리뷰 테스트용 숙소입니다");
        requestDto.setShortDescription("테스트 숙소");
        requestDto.setCity("서울");
        requestDto.setDistrict("강남구");
        requestDto.setTownship("역삼동");
        requestDto.setAddressDetail("테스트 주소 123");
        requestDto.setLatitude(new BigDecimal("37.5012"));
        requestDto.setLongitude(new BigDecimal("127.0396"));
        requestDto.setTransportInfo("지하철 2호선 역삼역");
        requestDto.setPhone("02-1234-5678");
        requestDto.setBusinessRegistrationNumber("123-45-67890");
        requestDto.setBusinessRegistrationImage("test-business-image.jpg");
        requestDto.setParkingInfo("무료 주차");
        requestDto.setCheckInTime("15:00");
        requestDto.setCheckOutTime("11:00");
        requestDto.setBankName("신한은행");
        requestDto.setAccountNumber("123-456-789");
        requestDto.setAccountHolder("테스트");
        requestDto.setAmenityIds(new ArrayList<>());
        requestDto.setThemeIds(new ArrayList<>());
        requestDto.setRooms(new ArrayList<>());

        return accommodationService.createAccommodation(testUserId, requestDto);
    }

    /**
     * 테스트용 확정된 예약 생성 (체크아웃 완료)
     */
    private Reservation createCompletedReservation(Long userId, Long accommodationId) {
        Reservation reservation = Reservation.builder()
                .userId(userId)
                .accommodationsId(accommodationId)
                .roomId(1L)
                .checkin(LocalDateTime.now().minusDays(3))
                .checkout(LocalDateTime.now().minusDays(1))  // 체크아웃 완료
                .stayNights(2)
                .guestCount(2)
                .reservationStatus(2)  // 확정 상태
                .totalAmountBeforeDc(100000)
                .couponDiscountAmount(0)
                .finalPaymentAmount(100000)
                .paymentStatus(1)
                .reserverName("테스트 예약자")
                .reserverPhone("010-1234-5678")
                .build();
        return reservationJpaRepository.save(reservation);
    }

    /**
     * 테스트용 예약 생성 (체크아웃 전)
     */
    private Reservation createFutureReservation(Long userId, Long accommodationId) {
        Reservation reservation = Reservation.builder()
                .userId(userId)
                .accommodationsId(accommodationId)
                .roomId(1L)
                .checkin(LocalDateTime.now().plusDays(1))
                .checkout(LocalDateTime.now().plusDays(3))  // 아직 체크아웃 안됨
                .stayNights(2)
                .guestCount(2)
                .reservationStatus(2)  // 확정 상태
                .totalAmountBeforeDc(100000)
                .couponDiscountAmount(0)
                .finalPaymentAmount(100000)
                .paymentStatus(1)
                .reserverName("테스트 예약자")
                .reserverPhone("010-1234-5678")
                .build();
        return reservationJpaRepository.save(reservation);
    }

    @Test
    @DisplayName("리뷰 등록 성공 테스트")
    void createReviewSuccessTest() {
        // given
        Long userId = 3L;
        createCompletedReservation(userId, testAccommodationId);
        entityManager.flush();

        ReviewCreateDto reviewCreateDto = ReviewCreateDto.builder()
                .accommodationsId(testAccommodationId)
                .rating(new BigDecimal("4.5"))
                .content("정말 좋은 숙소였습니다. 다음에도 또 방문하고 싶어요!")
                .visitDate("2024-12-25")
                .build();

        // when
        reviewService.createReview(userId, reviewCreateDto);
        entityManager.flush();

        // then
        boolean exists = reviewJpaRepository.existsByUserIdAndAccommodationsIdAndIsDeletedFalse(userId, testAccommodationId);
        assertThat(exists).isTrue();

        System.out.println("리뷰 등록 성공 - userId: " + userId + ", accommodationId: " + testAccommodationId);
    }

    @Test
    @DisplayName("이미지와 태그가 포함된 리뷰 등록 테스트")
    void createReviewWithImagesAndTagsTest() {
        // given
        Long userId = 4L;
        createCompletedReservation(userId, testAccommodationId);
        entityManager.flush();

        ReviewCreateDto reviewCreateDto = ReviewCreateDto.builder()
                .accommodationsId(testAccommodationId)
                .rating(new BigDecimal("5.0"))
                .content("최고의 숙소! 사진으로 남겨두고 싶을 정도였습니다.")
                .visitDate("2024-12-20")
                .imageUrls(List.of("base64Image1", "base64Image2"))
                .tagIds(List.of(1L, 2L, 3L))
                .build();

        // when
        reviewService.createReview(userId, reviewCreateDto);
        entityManager.flush();

        // then
        ReviewEntity savedReview = reviewJpaRepository.findAll().stream()
                .filter(r -> r.getUserId().equals(userId) && r.getAccommodationsId().equals(testAccommodationId))
                .findFirst()
                .orElseThrow();

        assertThat(savedReview.getImages()).hasSize(2);
        assertThat(savedReview.getContent()).isEqualTo("최고의 숙소! 사진으로 남겨두고 싶을 정도였습니다.");

        // 태그 저장 호출 검증
        Mockito.verify(reviewMapper).insertReviewTags(eq(savedReview.getReviewId()), eq(List.of(1L, 2L, 3L)));

        System.out.println("이미지와 태그가 포함된 리뷰 등록 성공 - 이미지 수: " + savedReview.getImages().size());
    }

    @Test
    @DisplayName("예약 내역 없을 때 예외 발생 테스트")
    void createReviewWithoutReservationTest() {
        // given
        Long userId = 5L;
        // testAccommodationId에 대해 예약을 생성하지 않음

        ReviewCreateDto reviewCreateDto = ReviewCreateDto.builder()
                .accommodationsId(testAccommodationId)
                .rating(new BigDecimal("4.0"))
                .content("좋았습니다")
                .visitDate("2024-12-25")
                .build();

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(userId, reviewCreateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("예약 내역이 없습니다");

        System.out.println("예약 내역 없음 예외 처리 성공");
    }

    @Test
    @DisplayName("체크아웃 전 리뷰 작성 시 예외 발생 테스트")
    void createReviewBeforeCheckoutTest() {
        // given
        Long userId = 6L;
        createFutureReservation(userId, testAccommodationId);  // 체크아웃 전 예약
        entityManager.flush();

        ReviewCreateDto reviewCreateDto = ReviewCreateDto.builder()
                .accommodationsId(testAccommodationId)
                .rating(new BigDecimal("4.0"))
                .content("아직 체크아웃 전인데 리뷰를 써봅니다")
                .visitDate("2024-12-25")
                .build();

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(userId, reviewCreateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("체크아웃이 완료된 예약 내역이 없습니다");

        System.out.println("체크아웃 전 리뷰 작성 예외 처리 성공");
    }

    @Test
    @DisplayName("중복 리뷰 작성 시 예외 발생 테스트")
    void createDuplicateReviewTest() {
        // given
        Long userId = 7L;
        createCompletedReservation(userId, testAccommodationId);
        entityManager.flush();

        ReviewCreateDto reviewCreateDto = ReviewCreateDto.builder()
                .accommodationsId(testAccommodationId)
                .rating(new BigDecimal("4.5"))
                .content("첫 번째 리뷰입니다")
                .visitDate("2024-12-25")
                .build();

        // 첫 번째 리뷰 등록
        reviewService.createReview(userId, reviewCreateDto);
        entityManager.flush();

        // when & then - 두 번째 리뷰 시도
        ReviewCreateDto duplicateReview = ReviewCreateDto.builder()
                .accommodationsId(testAccommodationId)
                .rating(new BigDecimal("3.0"))
                .content("두 번째 리뷰 시도")
                .visitDate("2024-12-26")
                .build();

        assertThatThrownBy(() -> reviewService.createReview(userId, duplicateReview))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 리뷰를 작성했습니다");

        System.out.println("중복 리뷰 작성 예외 처리 성공");
    }

    @Test
    @DisplayName("리뷰 작성 시 사용자 닉네임이 저장되는지 확인 테스트")
    void createReviewWithAuthorNameTest() {
        // given
        Long userId = 3L;  // testuser3 (test-base-data.sql)
        createCompletedReservation(userId, testAccommodationId);
        entityManager.flush();

        ReviewCreateDto reviewCreateDto = ReviewCreateDto.builder()
                .accommodationsId(testAccommodationId)
                .rating(new BigDecimal("4.0"))
                .content("닉네임 저장 테스트")
                .visitDate("2024-12-25")
                .build();

        // when
        reviewService.createReview(userId, reviewCreateDto);
        entityManager.flush();

        // then
        ReviewEntity savedReview = reviewJpaRepository.findAll().stream()
                .filter(r -> r.getUserId().equals(userId) && r.getAccommodationsId().equals(testAccommodationId))
                .findFirst()
                .orElseThrow();

        assertThat(savedReview.getAuthorName()).isEqualTo("testuser3");

        System.out.println("작성자 닉네임 저장 확인 - authorName: " + savedReview.getAuthorName());
    }
}
