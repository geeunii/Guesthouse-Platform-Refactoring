package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationCreateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationImageDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationUpdateRequestDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomCreateDto;
import com.ssg9th2team.geharbang.global.storage.ObjectStorageService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("integration-test")
@Transactional
@Sql(scripts = "/sql/test-base-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class AccommodationServiceTest extends com.ssg9th2team.geharbang.config.IntegrationTestConfig {

    @Autowired
    private AccommodationServiceImpl accommodationService;

    @MockBean
    private ObjectStorageService objectStorageService;

    @BeforeEach
    void setUp() {
        // ObjectStorageService Mock 설정 - 실제 업로드 대신 더미 URL 반환
        Mockito.when(objectStorageService.uploadBase64Image(Mockito.anyString(), Mockito.anyString()))
                .thenReturn("https://test-storage.com/test-image.jpg");
    }

    @Test
    @DisplayName("숙소 등록 통합 테스트 - 긴 이미지 문자열과 다양한 Enum 값 검증")
    void createAccommodationTest() {
        // given
        Long userId = 1L; // 더미 유저 ID (존재해야 함)

        // 아주 긴 Base64 문자열 생성 (600자 이상)
        String longBase64Image = "data:image/jpeg;base64," + "A".repeat(1000);

        AccommodationCreateRequestDto requestDto = new AccommodationCreateRequestDto();
        requestDto.setAccommodationsName("TDD 숙소");
        requestDto.setAccommodationsCategory("HOTEL"); // ENUM 테스트 (기존에 없던 값)
        requestDto.setAccommodationsDescription("테스트 설명");
        requestDto.setShortDescription("짧은 설명");
        requestDto.setCity("제주");
        requestDto.setDistrict("제주시");
        requestDto.setTownship("애월읍");
        requestDto.setAddressDetail("상세 주소 123");
        requestDto.setLatitude(new BigDecimal("33.1234567"));
        requestDto.setLongitude(new BigDecimal("126.1234567"));
        requestDto.setTransportInfo("셔틀 버스");
        requestDto.setPhone("010-1234-5678");
        requestDto.setBusinessRegistrationNumber("123-45-67890");
        requestDto.setBusinessRegistrationImage(longBase64Image); // 핵심: 긴 문자열 테스트

        requestDto.setParkingInfo("주차 가능");
        requestDto.setSns("instagram.com/test");
        requestDto.setCheckInTime("15:00");
        requestDto.setCheckOutTime("11:00");

        // 계좌 정보
        requestDto.setBankName("테스트은행");
        requestDto.setAccountNumber("123-456-789");
        requestDto.setAccountHolder("테스트예금주");

        // 이미지 (긴 문자열)
        AccommodationImageDto imageDto = new AccommodationImageDto();
        imageDto.setImageUrl(longBase64Image);
        imageDto.setImageType("banner");
        imageDto.setSortOrder(1);
        requestDto.setImages(Collections.singletonList(imageDto));

        // 객실 (긴 문자열)
        RoomCreateDto roomDto = new RoomCreateDto();
        roomDto.setRoomName("디럭스 룸");
        roomDto.setPrice(100000);
        roomDto.setWeekendPrice(120000);
        roomDto.setMinGuests(2);
        roomDto.setMaxGuests(4);
        roomDto.setRoomDescription("객실 설명");
        roomDto.setMainImageUrl(longBase64Image);
        roomDto.setBathroomCount(1);
        roomDto.setRoomType("ONDOL");
        roomDto.setBedCount(2);
        requestDto.setRooms(Collections.singletonList(roomDto));

        // 편의시설/테마 (ID는 DB에 존재하는 값이어야 함)
        // 없을 경우 null 포인터나 FK 에러가 날 수 있으니 빈 리스트로 먼저 테스트
        requestDto.setAmenityIds(new ArrayList<>());
        requestDto.setThemeIds(new ArrayList<>());

        // when
        Long savedId = accommodationService.createAccommodation(userId, requestDto);

        // then
        assertThat(savedId).isNotNull();
        System.out.println("숙소 등록 성공 ID: " + savedId);
    }

    @Test
    @DisplayName("숙소 상세 조회 테스트")
    void getAccommodationTest() {
        // given - 숙소 먼저 등록
        Long userId = 1L;
        Long savedId = createTestAccommodation(userId, "조회 테스트 숙소");

        // when
        AccommodationResponseDto response = accommodationService.getAccommodation(savedId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccommodationsId()).isEqualTo(savedId);
        assertThat(response.getAccommodationsName()).isEqualTo("조회 테스트 숙소");
        assertThat(response.getCity()).isEqualTo("제주");

        System.out.println("숙소 조회 성공 - " + response.getAccommodationsName());
    }

    @Test
    @DisplayName("숙소 수정 테스트")
    void updateAccommodationTest() {
        // given - 숙소 먼저 등록
        Long userId = 1L;
        Long savedId = createTestAccommodation(userId, "수정 전 숙소");

        // when - 숙소 정보 수정
        AccommodationUpdateRequestDto updateDto = new AccommodationUpdateRequestDto();
        updateDto.setAccommodationsName("수정 후 숙소");
        updateDto.setAccommodationsDescription("수정된 설명입니다");
        updateDto.setShortDescription("수정된 한줄소개");
        updateDto.setTransportInfo("수정된 교통정보");
        updateDto.setAccommodationStatus(1);
        updateDto.setParkingInfo("수정된 주차정보");
        updateDto.setSns("instagram.com/updated");
        updateDto.setPhone("010-9999-8888");
        updateDto.setCheckInTime("16:00");
        updateDto.setCheckOutTime("10:00");
        updateDto.setLatitude(new BigDecimal("33.5555"));
        updateDto.setLongitude(new BigDecimal("126.5555"));
        updateDto.setBankName("수정은행");
        updateDto.setAccountNumber("999-888-777");
        updateDto.setAccountHolder("수정예금주");
        updateDto.setAmenityIds(new ArrayList<>());
        updateDto.setThemeIds(new ArrayList<>());

        accommodationService.updateAccommodation(savedId, updateDto);

        // then
        AccommodationResponseDto updated = accommodationService.getAccommodation(savedId);
        assertThat(updated.getAccommodationsName()).isEqualTo("수정 후 숙소");
        assertThat(updated.getAccommodationsDescription()).isEqualTo("수정된 설명입니다");
        assertThat(updated.getPhone()).isEqualTo("010-9999-8888");
        assertThat(updated.getCheckInTime()).isEqualTo("16:00");

        System.out.println("숙소 수정 성공 - " + updated.getAccommodationsName());
    }

    @Test
    @DisplayName("숙소 삭제 테스트 - 예약 없는 경우")
    void deleteAccommodationWithoutReservationTest() {
        // given - 숙소 먼저 등록
        Long userId = 1L;
        Long savedId = createTestAccommodation(userId, "삭제할 숙소");

        // 등록 확인
        AccommodationResponseDto before = accommodationService.getAccommodation(savedId);
        assertThat(before).isNotNull();

        // when
        accommodationService.deleteAccommodation(savedId);

        // then
        AccommodationResponseDto after = accommodationService.getAccommodation(savedId);
        assertThat(after).isNull();

        System.out.println("숙소 삭제 성공 - ID: " + savedId);
    }

    @Test
    @DisplayName("숙소 등록 - 객실 포함 테스트")
    void createAccommodationWithRoomsTest() {
        // given
        Long userId = 1L;

        AccommodationCreateRequestDto requestDto = createBaseAccommodationDto("객실 포함 숙소");

        // 객실 2개 추가
        RoomCreateDto room1 = new RoomCreateDto();
        room1.setRoomName("스탠다드 룸");
        room1.setPrice(80000);
        room1.setWeekendPrice(100000);
        room1.setMinGuests(1);
        room1.setMaxGuests(2);
        room1.setRoomDescription("아늑한 스탠다드 룸");
        room1.setMainImageUrl("standard.jpg");
        room1.setBathroomCount(1);
        room1.setRoomType("BED");
        room1.setBedCount(1);
        room1.setRoomStatus(1);

        RoomCreateDto room2 = new RoomCreateDto();
        room2.setRoomName("디럭스 룸");
        room2.setPrice(150000);
        room2.setWeekendPrice(180000);
        room2.setMinGuests(2);
        room2.setMaxGuests(4);
        room2.setRoomDescription("넓은 디럭스 룸");
        room2.setMainImageUrl("deluxe.jpg");
        room2.setBathroomCount(2);
        room2.setRoomType("ONDOL");
        room2.setBedCount(2);
        room2.setRoomStatus(1);

        requestDto.setRooms(Arrays.asList(room1, room2));

        // when
        Long savedId = accommodationService.createAccommodation(userId, requestDto);

        // then
        assertThat(savedId).isNotNull();
        AccommodationResponseDto response = accommodationService.getAccommodation(savedId);
        assertThat(response.getRooms()).hasSize(2);

        System.out.println("객실 포함 숙소 등록 성공 - 객실 수: " + response.getRooms().size());
    }

    @Test
    @DisplayName("숙소 등록 - 편의시설/테마 연결 테스트")
    void createAccommodationWithAmenitiesAndThemesTest() {
        // given
        Long userId = 1L;

        AccommodationCreateRequestDto requestDto = createBaseAccommodationDto("편의시설 테스트 숙소");

        // 편의시설, 테마 ID는 DB에 존재하는 값이어야 함
        // 실제 테스트 시에는 DB에 있는 ID로 교체 필요
        requestDto.setAmenityIds(Arrays.asList(1L, 2L, 3L));
        requestDto.setThemeIds(Arrays.asList(1L, 2L));

        // when
        Long savedId = accommodationService.createAccommodation(userId, requestDto);

        // then
        assertThat(savedId).isNotNull();
        AccommodationResponseDto response = accommodationService.getAccommodation(savedId);
        assertThat(response.getAmenityIds()).isNotEmpty();
        assertThat(response.getThemeIds()).isNotEmpty();

        System.out.println("편의시설/테마 연결 성공 - Amenities: " + response.getAmenityIds().size()
                + ", Themes: " + response.getThemeIds().size());
    }

    @Test
    @DisplayName("숙소 카테고리 매핑 테스트 - GUESTHOUSE")
    void verifyGuesthouseCategoryMapping() {
        // given
        Long userId = 1L;
        AccommodationCreateRequestDto requestDto = createBaseAccommodationDto("게스트하우스 매핑 테스트");
        requestDto.setAccommodationsCategory("GUESTHOUSE"); // GUESTHOUSE 설정
        requestDto.setBusinessRegistrationImage(null); // Base64 Error 방지
        requestDto.setImages(null); // Base64 Error 방지

        // when
        Long savedId = accommodationService.createAccommodation(userId, requestDto);
        AccommodationResponseDto response = accommodationService.getAccommodation(savedId);

        // then
        assertThat(response).isNotNull();
        System.out.println("조회된 카테고리: " + response.getAccommodationsCategory());
        assertThat(response.getAccommodationsCategory()).isEqualTo("GUESTHOUSE");
    }

    // ============ Helper Methods ============

    private Long createTestAccommodation(Long userId, String name) {
        AccommodationCreateRequestDto requestDto = createBaseAccommodationDto(name);
        return accommodationService.createAccommodation(userId, requestDto);
    }

    private AccommodationCreateRequestDto createBaseAccommodationDto(String name) {
        AccommodationCreateRequestDto requestDto = new AccommodationCreateRequestDto();
        requestDto.setAccommodationsName(name);
        requestDto.setAccommodationsCategory("PENSION");
        requestDto.setAccommodationsDescription("테스트 숙소 설명");
        requestDto.setShortDescription("테스트 한줄소개");
        requestDto.setCity("제주");
        requestDto.setDistrict("제주시");
        requestDto.setTownship("애월읍");
        requestDto.setAddressDetail("테스트 주소 123");
        requestDto.setLatitude(new BigDecimal("33.1234567"));
        requestDto.setLongitude(new BigDecimal("126.1234567"));
        requestDto.setTransportInfo("셔틀 버스 운행");
        requestDto.setPhone("064-1234-5678");
        requestDto.setBusinessRegistrationNumber("123-45-67890");
        requestDto.setBusinessRegistrationImage("test-image.jpg");
        requestDto.setParkingInfo("무료 주차");
        requestDto.setSns("instagram.com/test");
        requestDto.setCheckInTime("15:00");
        requestDto.setCheckOutTime("11:00");
        requestDto.setBankName("제주은행");
        requestDto.setAccountNumber("123-456-789");
        requestDto.setAccountHolder("테스트예금주");
        requestDto.setAmenityIds(new ArrayList<>());
        requestDto.setThemeIds(new ArrayList<>());
        requestDto.setRooms(new ArrayList<>());
        return requestDto;
    }
}
