package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.*;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.AccommodationsCategory;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis.AccommodationMapper;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentRefundJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationResponseDto;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.service.ReservationService;
import com.ssg9th2team.geharbang.domain.room.dto.RoomCreateDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomResponseListDto;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import com.ssg9th2team.geharbang.domain.room.repository.mybatis.RoomMapper;
import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import com.ssg9th2team.geharbang.domain.theme.repository.ThemeRepository;
import com.ssg9th2team.geharbang.domain.wishlist.repository.mybatis.WishlistMapper;
import com.ssg9th2team.geharbang.global.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationServiceImpl implements AccommodationService {

    private final AccommodationMapper accommodationMapper;
    private final RoomMapper roomMapper;
    private final ObjectStorageService objectStorageService;
    private final ReservationJpaRepository reservationJpaRepository;
    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentRefundJpaRepository paymentRefundJpaRepository;
    private final WishlistMapper wishlistMapper;
    private final ThemeRepository themeRepository;


    // 숙소 등록
    @Override
    @Transactional
    public Long createAccommodation(Long userId, AccommodationCreateRequestDto createRequestDto) {
        
        // Base64 이미지 처리 - 네이버 클라우드 Object Storage에 업로드
        try {
            // 1. 사업자 등록증 이미지
            if (createRequestDto.getBusinessRegistrationImage() != null) {
                String savedUrl = objectStorageService.uploadBase64Image(
                        createRequestDto.getBusinessRegistrationImage(), "business");
                createRequestDto.setBusinessRegistrationImage(savedUrl);
            }

            // 2. 숙소 이미지 리스트
            if (createRequestDto.getImages() != null) {
                for (AccommodationImageDto img : createRequestDto.getImages()) {
                    if (img.getImageUrl() != null) {
                        String savedUrl = objectStorageService.uploadBase64Image(
                                img.getImageUrl(), "accommodations");
                        img.setImageUrl(savedUrl);
                    }
                }
            }


            // 3. 객실 대표 이미지
            if (createRequestDto.getRooms() != null) {
                log.info("객실 수: {}", createRequestDto.getRooms().size());
                for (RoomCreateDto room : createRequestDto.getRooms()) {
                    log.info("객실 이름: {}, mainImageUrl 존재: {}, 길이: {}",
                        room.getRoomName(),
                        room.getMainImageUrl() != null,
                        room.getMainImageUrl() != null ? room.getMainImageUrl().length() : 0);
                    if (room.getMainImageUrl() != null && !room.getMainImageUrl().isEmpty()) {
                        String savedUrl = objectStorageService.uploadBase64Image(
                                room.getMainImageUrl(), "rooms");
                        room.setMainImageUrl(savedUrl);
                    }
                }
            } else {
                log.info("객실 데이터 없음 (rooms is null)");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }


        // 정산계좌 먼저 등록
        // 계좌테이블이 부모이므로 계좌를 먼저 등록하고 계좌 아이디를 가지고 숙소를 등록
        AccountNumberDto accountNumberDto = new AccountNumberDto();
        accountNumberDto.setBankName(createRequestDto.getBankName());
        accountNumberDto.setAccountNumber(createRequestDto.getAccountNumber());
        accountNumberDto.setAccountHolder(createRequestDto.getAccountHolder());

        // 정산계좌 아이디 생성
        accommodationMapper.insertAccountNumber(accountNumberDto);

        // 생성된 정산계좌 아이디를 accountNumberId에 저장
        Long accountNumberId = accountNumberDto.getAccountNumberId();

        // 2. 숙소 엔티티 생성
        Accommodation accommodation = Accommodation.builder()
                .accountNumberId(accountNumberId) // 정산계좌 아이디 accountNumberId
                .userId(userId)
                .accommodationsName(createRequestDto.getAccommodationsName())  // 컬럼들은 Dto에서 가져오기
                .accommodationsCategory(AccommodationsCategory.valueOf(createRequestDto.getAccommodationsCategory()))  // dto로 받은 값(String)을 enum타입으로 변경
                .accommodationsDescription(createRequestDto.getAccommodationsDescription())
                .shortDescription(createRequestDto.getShortDescription())
                .city(createRequestDto.getCity())
                .district(createRequestDto.getDistrict())
                .township(createRequestDto.getTownship())
                .addressDetail(createRequestDto.getAddressDetail())
                .latitude(createRequestDto.getLatitude())
                .longitude(createRequestDto.getLongitude())
                .transportInfo(createRequestDto.getTransportInfo())
                .accommodationStatus(0)   // (1) = 운영중 , (0) = 운영 중지 (승인 대기)
                .approvalStatus(ApprovalStatus.PENDING)  // APPROVED = 승인 , PENDING = 검수중
                .createdAt(LocalDateTime.now())
                .phone(createRequestDto.getPhone())
                .businessRegistrationNumber(createRequestDto.getBusinessRegistrationNumber())
                .businessRegistrationImage(createRequestDto.getBusinessRegistrationImage())
                .parkingInfo(createRequestDto.getParkingInfo())
                .sns(createRequestDto.getSns())
                .checkInTime(createRequestDto.getCheckInTime())
                .checkOutTime(createRequestDto.getCheckOutTime())
                .build();


        // 3. 숙소 저장
        accommodationMapper.insertAccommodation(accommodation);

        // 숙소 저장 -> 숙소 아이디 생성 -> 숙소 아이디로 연관 테이블 저장
        Long accommodationsId = accommodation.getAccommodationsId();



        // ===================== 4. 연관 테이블 저장 ==========================

        // 이미지
        if (createRequestDto.getImages() != null && !createRequestDto.getImages().isEmpty()) {
            accommodationMapper.insertAccommodationImages(accommodationsId, createRequestDto.getImages());
        }

        // 편의시설
        if (createRequestDto.getAmenityIds() != null && !createRequestDto.getAmenityIds().isEmpty()) {
            accommodationMapper.insertAccommodationAmenities(accommodationsId, createRequestDto.getAmenityIds());
        }

        // 테마
        if (createRequestDto.getThemeIds() != null && !createRequestDto.getThemeIds().isEmpty()) {
            validateThemeIds(createRequestDto.getThemeIds());
            accommodationMapper.insertAccommodationThemes(accommodationsId, createRequestDto.getThemeIds());
        }

        // 객실
        if (createRequestDto.getRooms() != null && !createRequestDto.getRooms().isEmpty()) {
            roomMapper.insertRooms(accommodationsId, createRequestDto.getRooms());

            // 객실 등록 후 숙소의 최소 가격 업데이트
            accommodationMapper.updateMinPrice(accommodationsId);
        }

        return accommodationsId;
    }



    // 숙소 상세조회
    @Override
    public AccommodationResponseDto getAccommodation(Long accommodationsId) {
        return accommodationMapper.selectAccommodationById(accommodationsId);
    }



    // 숙소 수정
    @Override
    @Transactional
    public void updateAccommodation(Long accommodationsId, AccommodationUpdateRequestDto updateRequestDto) {
        // 1. 숙소 기본 정보 업데이트
        Accommodation accommodation = Accommodation.builder()
                .accommodationsId(accommodationsId)
                .accommodationsName(updateRequestDto.getAccommodationsName())
                .accommodationsDescription(updateRequestDto.getAccommodationsDescription())
                .shortDescription(updateRequestDto.getShortDescription())
                .transportInfo(updateRequestDto.getTransportInfo())
                .accommodationStatus(updateRequestDto.getAccommodationStatus())
                .parkingInfo(updateRequestDto.getParkingInfo())
                .sns(updateRequestDto.getSns())
                .phone(updateRequestDto.getPhone())
                .checkInTime(updateRequestDto.getCheckInTime())
                .checkOutTime(updateRequestDto.getCheckOutTime())
                .latitude(updateRequestDto.getLatitude())
                .longitude(updateRequestDto.getLongitude())
                .build();

        // 업데이트 쿼리에 숙소 아이디 , 업데이트 된 숙소 정보 주입
        accommodationMapper.updateAccommodation(accommodationsId, accommodation);

        // 2. 연관 데이터 업데이트 (삭제 후 재등록)
        
        // 편의시설
        // 편의시설 수정된 값이 없다면 이 로직 실행 안됨
        if (updateRequestDto.getAmenityIds() != null) {  // 수정 요청이 들어오면
            accommodationMapper.deleteAccommodationAmenities(accommodationsId);  // 전체 삭제한다 (삭제만 하고 추가 안 할시 이 로직 실행)

            if (!updateRequestDto.getAmenityIds().isEmpty()) { // 삭제 후 추가 할때 이 로직 (삭제 후 재등록)
                accommodationMapper.insertAccommodationAmenities(accommodationsId, updateRequestDto.getAmenityIds()); // 다시 등록
            }
        }

        // 테마
        if (updateRequestDto.getThemeIds() != null) {
            accommodationMapper.deleteAccommodationThemes(accommodationsId);
            if (!updateRequestDto.getThemeIds().isEmpty()) {
                validateThemeIds(updateRequestDto.getThemeIds());
                accommodationMapper.insertAccommodationThemes(accommodationsId, updateRequestDto.getThemeIds());
            }
        }

        // 이미지
        if (updateRequestDto.getImages() != null) {
            // 이미지 업로드 로직 추가
            try {
                for (AccommodationImageDto img : updateRequestDto.getImages()) {
                    if (img.getImageUrl() != null) {
                        String savedUrl = objectStorageService.uploadBase64Image(
                                img.getImageUrl(), "accommodations");
                        img.setImageUrl(savedUrl);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("숙소 이미지 수정 중 오류가 발생했습니다: " + e.getMessage());
            }

            // 이미지도 삭제 후 재등록
            accommodationMapper.deleteAccommodationImages(accommodationsId);
            if (!updateRequestDto.getImages().isEmpty()) {
                accommodationMapper.insertAccommodationImages(accommodationsId, updateRequestDto.getImages());
            }
        }


        // 먼저 현재 숙소 정보를 조회하여 연결된 계좌 ID(accountNumberId)를 가져옵니다.
        // 숙소 정보 업데이트는 숙소 테이블(자기자신) 업데이트니까 따로 조회가 필요 하지 않음
        // 객실, 정산계좌는 숙소에 포함된 외래키니까 숙소 정보를 조회하고 원하는 값을 가져와야함
        AccommodationResponseDto accountNumber = accommodationMapper.selectAccommodationById(accommodationsId);
        Long accountNumberId = accountNumber.getAccountNumberId();

        AccountNumberDto accountNumberDto = new AccountNumberDto();
        accountNumberDto.setBankName(updateRequestDto.getBankName());
        accountNumberDto.setAccountNumber(updateRequestDto.getAccountNumber());
        accountNumberDto.setAccountHolder(updateRequestDto.getAccountHolder());

        // 그 ID를 사용하여 정산 계좌 테이블을 업데이트합니다.
        accommodationMapper.updateAccountNumber(accountNumberId, accountNumberDto);

        // 3. 객실 동기화 (삭제 및 추가/수정)
        
        // 3-1. 현재 DB에 저장된 객실 목록 조회 (삭제 대상 식별용)
        List<RoomResponseListDto> currentRooms = accommodationMapper.selectRoomsByAccommodationId(accommodationsId);
        
        // 3-2. 요청된 객실 ID 목록 추출
        List<Long> requestedRoomIds = new ArrayList<>();
        if (updateRequestDto.getRooms() != null) {
            for (AccommodationUpdateRequestDto.RoomData r : updateRequestDto.getRooms()) {
                if (r.getRoomId() != null) {
                    requestedRoomIds.add(r.getRoomId());
                }
            }
        }

        // 3-3. DB에는 있는데 요청에는 없는 ID 삭제
        for (RoomResponseListDto currentRoom : currentRooms) {
            if (!requestedRoomIds.contains(currentRoom.getRoomId())) {
                // 객실 삭제 전 예약 확인 및 처리
                List<Reservation> roomReservations = reservationJpaRepository.findByRoomId(currentRoom.getRoomId());
                LocalDateTime now = LocalDateTime.now();
                boolean hasActiveReservation = roomReservations.stream()
                        .anyMatch(r -> r.getReservationStatus() == 2 // 2: 확정
                                        && r.getCheckout().isAfter(now)); // 현재 시간보다 체크아웃이 미래인 경우만 Active로 간주

                if (hasActiveReservation) {
                    Reservation activeRes = roomReservations.stream()
                            .filter(r -> r.getReservationStatus() == 2 && r.getCheckout().isAfter(now))
                            .findFirst()
                            .orElse(null);
                    String debugInfo = activeRes != null ? "(예약ID: " + activeRes.getId() + ", 상태: " + activeRes.getReservationStatus() + ", 체크아웃: " + activeRes.getCheckout() + ")" : "";
                    throw new IllegalStateException("아직 종료되지 않은 예약이 있는 객실은 삭제할 수 없습니다. " + debugInfo);
                }

                // 관련 데이터 삭제 (지난 예약, 취소된 예약 등)
                if (!roomReservations.isEmpty()) {
                    List<Long> reservationIds = roomReservations.stream().map(Reservation::getId).toList();
                    
                    if (!reservationIds.isEmpty()) {
                        // 1. Payment & Refund 삭제
                        List<Payment> payments = paymentJpaRepository.findByReservationIdIn(reservationIds);
                        List<Long> paymentIds = payments.stream().map(Payment::getId).toList();

                        if (!paymentIds.isEmpty()) {
                            paymentRefundJpaRepository.deleteByPaymentIdIn(paymentIds);
                            paymentRefundJpaRepository.flush();
                        }


                        paymentJpaRepository.deleteByReservationIdIn(reservationIds);
                        paymentJpaRepository.flush();
                    }

                    // 3. Reservation 삭제
                    reservationJpaRepository.deleteAllInBatch(roomReservations);
                    reservationJpaRepository.flush();
                }

                roomMapper.deleteRoom(accommodationsId, currentRoom.getRoomId());
            }
        }

        // 3-4. 객실 추가/수정
        if (updateRequestDto.getRooms() != null) {
            for (AccommodationUpdateRequestDto.RoomData roomDto : updateRequestDto.getRooms()) {
                // 객실 이미지 업로드 로직 추가
                try {
                    if (roomDto.getMainImageUrl() != null) {
                        String savedUrl = objectStorageService.uploadBase64Image(
                                roomDto.getMainImageUrl(), "rooms");
                        roomDto.setMainImageUrl(savedUrl);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("객실 이미지 수정 중 오류가 발생했습니다: " + e.getMessage());
                }

                Room room = Room.builder()
                        .accommodationsId(accommodationsId)
                        .roomName(roomDto.getRoomName())
                        .price(roomDto.getPrice())
                        .weekendPrice(roomDto.getWeekendPrice())
                        .minGuests(roomDto.getMinGuests())
                        .maxGuests(roomDto.getMaxGuests())
                        .roomDescription(roomDto.getRoomDescription())
                        .mainImageUrl(roomDto.getMainImageUrl())
                        .bathroomCount(roomDto.getBathroomCount())
                        .roomType(roomDto.getRoomType())
                        .bedCount(roomDto.getBedCount())
                        .roomStatus(roomDto.getRoomStatus() != null ? roomDto.getRoomStatus() : 1)
                        .build();

                if (roomDto.getRoomId() != null) {
                    roomMapper.updateRoom(accommodationsId, roomDto.getRoomId(), room);
                } else {
                    roomMapper.insertRoom(room);
                }
            }
            // 최저가 갱신
            accommodationMapper.updateMinPrice(accommodationsId);
        }
    }


    // 숙소 삭제
    @Override
    @Transactional
    public void deleteAccommodation(Long accommodationsId) {
        // 예약 확인 ( status = 2 = 예약 완료)
        // 숙소 결제 정보가 있는지 조회
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> reservations = reservationJpaRepository.findByAccommodationsId(accommodationsId);
        boolean hasActiveReservation = reservations.stream()
                .anyMatch(r -> r.getReservationStatus() == 2 && r.getCheckout().isAfter(now)); // 확정된 예약 중 아직 체크아웃 하지 않은 예약이 있는지 확인

        if(hasActiveReservation) {
            throw new IllegalStateException("예약된 정보가 있어 삭제할 수 없습니다.");
        }

        // 연관된 예약 정보 삭제 (취소/완료된 예약 등 Active하지 않은 예약들)
        if (!reservations.isEmpty()) {
            
            // 1. Payment 삭제 전에 PaymentRefund 삭제 필요
            List<Long> reservationIds = reservations.stream().map(Reservation::getId).toList();
            if(!reservationIds.isEmpty()){
                List<Payment> payments = paymentJpaRepository.findByReservationIdIn(reservationIds);
                List<Long> paymentIds = payments.stream().map(Payment::getId).toList();
                
                if (!paymentIds.isEmpty()) {
                    paymentRefundJpaRepository.deleteByPaymentIdIn(paymentIds);
                    paymentRefundJpaRepository.flush(); // 환불 데이터 삭제 반영
                }

                paymentJpaRepository.deleteByReservationIdIn(reservationIds);
                paymentJpaRepository.flush(); // 결제 데이터 삭제 반영
            }

            reservationJpaRepository.deleteAllInBatch(reservations);
            reservationJpaRepository.flush(); // 예약 데이터 삭제 반영
        }
        
        // 4. Wishlist 삭제 (FK_WISHLIST_ACC 제약조건 해결)
        wishlistMapper.deleteWishlistByAccommodationId(accommodationsId);

        accommodationMapper.deleteAccommodation(accommodationsId);

    }

    // 숙소 일괄 삭제
    @Override
    @Transactional
    public void deleteAccommodations(List<Long> accommodationIds) {
        if (accommodationIds == null || accommodationIds.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        List<Reservation> reservations = reservationJpaRepository.findByAccommodationsIdIn(accommodationIds);
        boolean hasActiveReservation = reservations.stream()
                .anyMatch(r -> r.getReservationStatus() == 2 && r.getCheckout().isAfter(now));

        if(hasActiveReservation) {
            throw new IllegalStateException("예약된 정보가 있어 삭제할 수 없습니다.");
        }

        // 연관된 예약 정보 삭제
        if (!reservations.isEmpty()) {
            List<Long> reservationIds = reservations.stream().map(Reservation::getId).toList();
            if(!reservationIds.isEmpty()){
                List<Payment> payments = paymentJpaRepository.findByReservationIdIn(reservationIds);
                List<Long> paymentIds = payments.stream().map(Payment::getId).toList();

                if (!paymentIds.isEmpty()) {
                    paymentRefundJpaRepository.deleteByPaymentIdIn(paymentIds);
                    paymentRefundJpaRepository.flush();
                }

                paymentJpaRepository.deleteByReservationIdIn(reservationIds);
                paymentJpaRepository.flush();
            }

            reservationJpaRepository.deleteAllInBatch(reservations);
            reservationJpaRepository.flush();
        }

        // 위시리스트 삭제
        wishlistMapper.deleteWishlistByAccommodationIdIn(accommodationIds);

        // 연관 테이블 삭제
        accommodationMapper.deleteAccommodationAmenitiesIn(accommodationIds);
        accommodationMapper.deleteAccommodationThemesIn(accommodationIds);
        accommodationMapper.deleteAccommodationImagesIn(accommodationIds);

        // 숙소 삭제
        accommodationMapper.deleteAccommodations(accommodationIds);
    }

    private void validateThemeIds(List<Long> themeIds) {
        if (themeIds == null || themeIds.isEmpty()) return;
        List<Long> distinctIds = themeIds.stream().distinct().toList();
        List<Long> existingIds = themeRepository.findAllById(distinctIds).stream()
                .map(Theme::getId)
                .toList();
        if (existingIds.size() != distinctIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 테마 ID가 포함되어 있습니다.");
        }
    }
}
