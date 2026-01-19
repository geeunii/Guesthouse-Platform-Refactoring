package com.ssg9th2team.geharbang.domain.reservation.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis.AccommodationMapper;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.payment.service.PaymentService;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationRequestDto;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(ConcurrencyTest.Config.class) // 내부 설정 클래스 Import
@Sql(scripts = "/sql/test-base-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationJpaRepository reservationRepository;

    @Autowired
    private RoomJpaRepository roomRepository;

    @Autowired
    private AccommodationJpaRepository accommodationRepository;

    @Autowired
    private UserRepository userRepository;

    private Long roomId;
    private Long accommodationId;

    // Test 전용 설정: Service 빈을 수동으로 등록하여 의존성 주입 문제 해결
    @TestConfiguration
    static class Config {
        @MockBean
        AccommodationMapper accommodationMapper;
        @MockBean
        ReviewJpaRepository reviewJpaRepository;
        @MockBean
        PaymentService paymentService;

        @Bean
        public ReservationService reservationService(
                ReservationJpaRepository reservationRepository,
                AccommodationJpaRepository accommodationRepository,
                AccommodationMapper accommodationMapper,
                UserRepository userRepository,
                ReviewJpaRepository reviewJpaRepository,
                PaymentService paymentService,
                RoomJpaRepository roomJpaRepository) {
            return new ReservationServiceImpl(
                    reservationRepository,
                    accommodationRepository,
                    accommodationMapper,
                    userRepository,
                    reviewJpaRepository,
                    paymentService,
                    roomJpaRepository);
        }
    }

    @BeforeEach
    void setUp() {
        // [SecurityContext Mocking]
        com.ssg9th2team.geharbang.domain.auth.entity.User user = com.ssg9th2team.geharbang.domain.auth.entity.User
                .builder()
                .email("test@example.com")
                .name("테스터")
                .role(com.ssg9th2team.geharbang.domain.auth.entity.UserRole.USER)
                .build();
        userRepository.save(user);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("test@example.com", null,
                Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 1. Accommodation 생성
        Accommodation accommodation = Accommodation.builder()
                .accommodationsName("테스트 숙소")
                .city("서울").district("강남구").township("역삼동").addressDetail("123-45")
                .latitude(java.math.BigDecimal.valueOf(37.5))
                .longitude(java.math.BigDecimal.valueOf(127.0))
                .accommodationsCategory(
                        com.ssg9th2team.geharbang.domain.accommodation.entity.AccommodationsCategory.HOTEL)
                .accommodationsDescription("테스트 설명")
                .shortDescription("짧은 설명")
                .transportInfo("교통")
                .phone("010-0000-0000")
                .businessRegistrationNumber("1234567890")
                .parkingInfo("주차가능")
                .checkInTime("15:00")
                .checkOutTime("11:00")
                .accountNumberId(1L)
                .userId(1L)
                .build();
        accommodation = accommodationRepository.save(accommodation);
        accommodationId = accommodation.getAccommodationsId();

        // 2. Room 생성
        Room room = Room.builder()
                .accommodationsId(accommodationId)
                .roomName("테스트 객실")
                .maxGuests(10)
                .minGuests(1)
                .price(10000)
                .roomStatus(1)
                .build();
        room = roomRepository.save(room);
        roomId = room.getRoomId();
    }

    @Test
    @DisplayName("동시성 제어: 같은 객실/날짜에 100명이 동시에 예약하면 직렬화되어 1명만 성공해야 한다.")
    void concurrencyReservationTest() throws InterruptedException {
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        ReservationRequestDto requestDto = new ReservationRequestDto(
                accommodationId,
                roomId,
                1L,
                Instant.now().plus(1, ChronoUnit.DAYS),
                Instant.now().plus(2, ChronoUnit.DAYS),
                1,
                10000,
                null, // userCouponId
                0, // couponDiscount
                "테스터",
                "010-1234-5678");

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    reservationService.createReservation(requestDto);
                    successCount.getAndIncrement();
                } catch (Exception e) {
                    failCount.getAndIncrement();
                } finally { // 항상 countDown
                    latch.countDown();
                }
            });
        }

        latch.await();

        System.out.println("성공한 예약 수: " + successCount.get());
        System.out.println("실패한 예약 수: " + failCount.get());

        // 검증
        assertThat(successCount.get()).isEqualTo(1);
    }
}
