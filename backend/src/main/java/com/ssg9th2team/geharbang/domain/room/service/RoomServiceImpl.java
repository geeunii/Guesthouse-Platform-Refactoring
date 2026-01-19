package com.ssg9th2team.geharbang.domain.room.service;

import com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis.AccommodationMapper;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.room.dto.RoomCreateDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomResponseDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomUpdateDto;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import com.ssg9th2team.geharbang.domain.room.repository.mybatis.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomMapper roomMapper;
    private final AccommodationMapper accommodationMapper;
    private final ReservationJpaRepository reservationJpaRepository;

    // 추가 객실 등록
    @Override
    @Transactional
    public Long createRoom(Long accommodationsId, RoomCreateDto roomCreateDto) {
        Room room = Room.builder()
                .accommodationsId(accommodationsId)
                .roomName(roomCreateDto.getRoomName())
                .price(roomCreateDto.getPrice())
                .weekendPrice(roomCreateDto.getWeekendPrice())
                .minGuests(roomCreateDto.getMinGuests())
                .maxGuests(roomCreateDto.getMaxGuests())
                .roomDescription(roomCreateDto.getRoomDescription())
                .roomIntroduction(roomCreateDto.getRoomIntroduction())
                .mainImageUrl(roomCreateDto.getMainImageUrl())
                .roomStatus(roomCreateDto.getRoomStatus() != null ? roomCreateDto.getRoomStatus() : 1)
                .bathroomCount(roomCreateDto.getBathroomCount())
                .roomType(roomCreateDto.getRoomType())
                .bedCount(roomCreateDto.getBedCount())
                .build();

        roomMapper.insertRoom(room);

        // 객실 등록 후 숙소의 최소 가격 업데이트
        accommodationMapper.updateMinPrice(accommodationsId);

        return room.getRoomId();
    }

    // 객실 수정
    @Override
    @Transactional
    public void updateRoom(Long accommodationsId, Long roomId, RoomUpdateDto roomUpdateDto) {
        Room room = Room.builder()
                .roomName(roomUpdateDto.getRoomName())
                .price(roomUpdateDto.getPrice())
                .weekendPrice(roomUpdateDto.getWeekendPrice())
                .minGuests(roomUpdateDto.getMinGuests())
                .maxGuests(roomUpdateDto.getMaxGuests())
                .roomDescription(roomUpdateDto.getRoomDescription())
                .roomIntroduction(roomUpdateDto.getRoomIntroduction())
                .mainImageUrl(roomUpdateDto.getMainImageUrl())
                .bathroomCount(roomUpdateDto.getBathroomCount())
                .roomType(roomUpdateDto.getRoomType())
                .bedCount(roomUpdateDto.getBedCount())
                .build();

        roomMapper.updateRoom(accommodationsId, roomId, room);

        // 객실 수정 후 숙소의 최소 가격 업데이트
        accommodationMapper.updateMinPrice(accommodationsId);
    }

    // 객실 삭제
    @Override
    @Transactional
    public void deleteRoom(Long accommodationsId, Long roomId) {
        // 해당 객실의 예약정보 있는지 확인 (아직 체크아웃 안 된 확정 예약만)
        List<Reservation> reservations = reservationJpaRepository.findByRoomId(roomId);
        LocalDateTime now = LocalDateTime.now();

        boolean hasActiveReservation = reservations.stream()
                .anyMatch(r -> r.getReservationStatus() == 2 && r.getCheckout().isAfter(now));

        // 아직 체크아웃 안 된 확정 예약이 있다면
        if (hasActiveReservation) {
            throw new IllegalStateException("예약된 정보가 있어 삭제할 수 없습니다");
        }

        // 예약이 없다면 객실 삭제
        roomMapper.deleteRoom(accommodationsId, roomId);
        // 객실 최소금액 업데이트
        accommodationMapper.updateMinPrice(accommodationsId);
    }

    // 객실 상세 조회
    @Override
    public RoomResponseDto getRoom(Long accommodationsId, Long roomId) {
        return roomMapper.selectRoomById(accommodationsId, roomId);
    }
}
