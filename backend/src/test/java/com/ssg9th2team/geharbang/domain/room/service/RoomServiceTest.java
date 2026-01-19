package com.ssg9th2team.geharbang.domain.room.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationCreateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.accommodation.service.AccommodationServiceImpl;
import com.ssg9th2team.geharbang.domain.room.dto.RoomCreateDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomResponseDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomUpdateDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@Transactional
@Sql(scripts = "/sql/test-base-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@SpringBootTest
public class RoomServiceTest {

    @MockBean
    protected com.ssg9th2team.geharbang.domain.ocr.service.OcrService ocrService;

    @Autowired
    private RoomServiceImpl roomService;

    @Autowired
    private AccommodationServiceImpl accommodationService;

    @MockBean
    private ObjectStorageService objectStorageService;

    private Long testAccommodationId;

    @BeforeEach
    void setUp() {
        // ObjectStorageService Mock 설정
        Mockito.when(objectStorageService.uploadBase64Image(Mockito.anyString(), Mockito.anyString()))
                .thenReturn("https://test-storage.com/test-image.jpg");
        // 테스트용 숙소 먼저 생성
        Long userId = 1L;

        AccommodationCreateRequestDto requestDto = new AccommodationCreateRequestDto();
        requestDto.setAccommodationsName("객실 테스트용 숙소");
        requestDto.setAccommodationsCategory("PENSION");
        requestDto.setAccommodationsDescription("객실 테스트를 위한 숙소입니다");
        requestDto.setShortDescription("테스트 숙소");
        requestDto.setCity("서울");
        requestDto.setDistrict("강남구");
        requestDto.setTownship("역삼동");
        requestDto.setAddressDetail("테스트 주소 123");
        requestDto.setLatitude(new BigDecimal("37.5665"));
        requestDto.setLongitude(new BigDecimal("126.9780"));
        requestDto.setTransportInfo("지하철 2호선");
        requestDto.setPhone("02-1234-5678");
        requestDto.setBusinessRegistrationNumber("123-45-67890");
        requestDto.setBusinessRegistrationImage("test-image-url");
        requestDto.setParkingInfo("주차 가능");
        requestDto.setCheckInTime("15:00");
        requestDto.setCheckOutTime("11:00");
        requestDto.setBankName("테스트은행");
        requestDto.setAccountNumber("123-456-789");
        requestDto.setAccountHolder("테스트");
        requestDto.setAmenityIds(new ArrayList<>());
        requestDto.setThemeIds(new ArrayList<>());
        requestDto.setRooms(new ArrayList<>());

        testAccommodationId = accommodationService.createAccommodation(userId, requestDto);
    }

    @Test
    @DisplayName("객실 등록 테스트")
    void createRoomTest() {
        // given
        RoomCreateDto roomDto = new RoomCreateDto();
        roomDto.setRoomName("디럭스 룸");
        roomDto.setPrice(150000);
        roomDto.setWeekendPrice(180000);
        roomDto.setMinGuests(2);
        roomDto.setMaxGuests(4);
        roomDto.setRoomDescription("넓고 쾌적한 디럭스 룸입니다");
        roomDto.setMainImageUrl("test-room-image.jpg");
        roomDto.setBathroomCount(1);
        roomDto.setRoomType("ONDOL");
        roomDto.setBedCount(2);
        roomDto.setRoomStatus(1);

        // when
        Long roomId = roomService.createRoom(testAccommodationId, roomDto);

        // then
        assertThat(roomId).isNotNull();

        RoomResponseDto savedRoom = roomService.getRoom(testAccommodationId, roomId);
        assertThat(savedRoom).isNotNull();
        assertThat(savedRoom.getRoomName()).isEqualTo("디럭스 룸");
        assertThat(savedRoom.getPrice()).isEqualTo(150000);

        System.out.println("객실 등록 성공 - Room ID: " + roomId);
    }

    @Test
    @DisplayName("객실 등록 - 운영상태 비활성화 테스트")
    void createRoomWithInactiveStatusTest() {
        // given
        RoomCreateDto roomDto = new RoomCreateDto();
        roomDto.setRoomName("비활성화 객실");
        roomDto.setPrice(100000);
        roomDto.setWeekendPrice(120000);
        roomDto.setMinGuests(1);
        roomDto.setMaxGuests(2);
        roomDto.setRoomDescription("비활성화 상태로 등록되는 객실");
        roomDto.setMainImageUrl("test-image.jpg");
        roomDto.setBathroomCount(1);
        roomDto.setRoomType("BED");
        roomDto.setBedCount(1);
        roomDto.setRoomStatus(0); // 비활성화

        // when
        Long roomId = roomService.createRoom(testAccommodationId, roomDto);

        // then
        assertThat(roomId).isNotNull();
        RoomResponseDto savedRoom = roomService.getRoom(testAccommodationId, roomId);
        assertThat(savedRoom.getRoomStatus()).isEqualTo(0);

        System.out.println("비활성화 객실 등록 성공 - Room ID: " + roomId + ", Status: " + savedRoom.getRoomStatus());
    }

    @Test
    @DisplayName("객실 수정 테스트")
    void updateRoomTest() {
        // given - 먼저 객실 등록
        RoomCreateDto createDto = new RoomCreateDto();
        createDto.setRoomName("수정 전 객실");
        createDto.setPrice(100000);
        createDto.setWeekendPrice(120000);
        createDto.setMinGuests(2);
        createDto.setMaxGuests(4);
        createDto.setRoomDescription("수정 전 설명");
        createDto.setMainImageUrl("before-image.jpg");
        createDto.setBathroomCount(1);
        createDto.setRoomType("ONDOL");
        createDto.setBedCount(1);
        createDto.setRoomStatus(1);

        Long roomId = roomService.createRoom(testAccommodationId, createDto);

        // when - 객실 수정
        RoomUpdateDto updateDto = RoomUpdateDto.builder()
                .roomName("수정 후 객실")
                .price(200000)
                .weekendPrice(250000)
                .minGuests(2)
                .maxGuests(6)
                .roomDescription("수정 후 설명")
                .mainImageUrl("after-image.jpg")
                .bathroomCount(2)
                .roomType("BED")
                .bedCount(3)
                .roomStatus(1)
                .build();

        roomService.updateRoom(testAccommodationId, roomId, updateDto);

        // then
        RoomResponseDto updatedRoom = roomService.getRoom(testAccommodationId, roomId);
        assertThat(updatedRoom.getRoomName()).isEqualTo("수정 후 객실");
        assertThat(updatedRoom.getPrice()).isEqualTo(200000);
        assertThat(updatedRoom.getMaxGuests()).isEqualTo(6);

        System.out.println("객실 수정 성공 - Room ID: " + roomId);
    }

    @Test
    @DisplayName("객실 삭제 테스트 - 예약 없는 경우")
    void deleteRoomWithoutReservationTest() {
        // given
        RoomCreateDto roomDto = new RoomCreateDto();
        roomDto.setRoomName("삭제할 객실");
        roomDto.setPrice(100000);
        roomDto.setWeekendPrice(120000);
        roomDto.setMinGuests(2);
        roomDto.setMaxGuests(4);
        roomDto.setRoomDescription("삭제 테스트용 객실");
        roomDto.setMainImageUrl("delete-test.jpg");
        roomDto.setBathroomCount(1);
        roomDto.setRoomType("ONDOL");
        roomDto.setBedCount(1);
        roomDto.setRoomStatus(1);

        Long roomId = roomService.createRoom(testAccommodationId, roomDto);

        // when
        roomService.deleteRoom(testAccommodationId, roomId);

        // then
        RoomResponseDto deletedRoom = roomService.getRoom(testAccommodationId, roomId);
        assertThat(deletedRoom).isNull();

        System.out.println("객실 삭제 성공 - Room ID: " + roomId);
    }

    @Test
    @DisplayName("객실 상세 조회 테스트")
    void getRoomDetailTest() {
        // given
        RoomCreateDto roomDto = new RoomCreateDto();
        roomDto.setRoomName("조회 테스트 객실");
        roomDto.setPrice(180000);
        roomDto.setWeekendPrice(220000);
        roomDto.setMinGuests(2);
        roomDto.setMaxGuests(5);
        roomDto.setRoomDescription("상세 조회 테스트용 객실입니다");
        roomDto.setMainImageUrl("detail-test.jpg");
        roomDto.setBathroomCount(2);
        roomDto.setRoomType("MIXED");
        roomDto.setBedCount(3);
        roomDto.setRoomStatus(1);

        Long roomId = roomService.createRoom(testAccommodationId, roomDto);

        // when
        RoomResponseDto room = roomService.getRoom(testAccommodationId, roomId);

        // then
        assertThat(room).isNotNull();
        assertThat(room.getRoomId()).isEqualTo(roomId);
        assertThat(room.getRoomName()).isEqualTo("조회 테스트 객실");
        assertThat(room.getPrice()).isEqualTo(180000);
        assertThat(room.getWeekendPrice()).isEqualTo(220000);
        assertThat(room.getMinGuests()).isEqualTo(2);
        assertThat(room.getMaxGuests()).isEqualTo(5);

        System.out.println("객실 조회 성공 - " + room.getRoomName());
    }

    @Test
    @DisplayName("객실 등록 - 소개글(roomIntroduction) 매핑 테스트")
    void createRoomIntroductionTest() {
        // given
        RoomCreateDto roomDto = new RoomCreateDto();
        roomDto.setRoomName("소개글 테스트 룸");
        roomDto.setPrice(150000);
        roomDto.setWeekendPrice(180000);
        roomDto.setMinGuests(2);
        roomDto.setMaxGuests(4);
        roomDto.setRoomDescription("기존 설명");
        roomDto.setRoomIntroduction("새로운 소개글입니다. 매핑 확인용.");
        roomDto.setMainImageUrl("test.jpg");
        roomDto.setBathroomCount(1);
        roomDto.setRoomType("ONDOL");
        roomDto.setBedCount(2);
        roomDto.setRoomStatus(1);

        // when
        Long roomId = roomService.createRoom(testAccommodationId, roomDto);

        // then
        RoomResponseDto savedRoom = roomService.getRoom(testAccommodationId, roomId);
        assertThat(savedRoom.getRoomIntroduction()).isEqualTo("새로운 소개글입니다. 매핑 확인용.");
        System.out.println("Introduction 매핑 확인 완료: " + savedRoom.getRoomIntroduction());

        // Check via AccommodationService to verify RoomList response
        AccommodationResponseDto accDto = accommodationService.getAccommodation(testAccommodationId);
        assertThat(accDto.getRooms()).isNotEmpty();
        // find variable roomId
        boolean found = accDto.getRooms().stream()
                .anyMatch(r -> r.getRoomId().equals(roomId) && "새로운 소개글입니다. 매핑 확인용.".equals(r.getRoomIntroduction()));
        assertThat(found).isTrue();
        System.out.println("Accommodation 조회 시 객실 리스트에도 Introduction 포함 확인 완료");
    }
}
