package com.ssg9th2team.geharbang.domain.room.service;

import com.ssg9th2team.geharbang.domain.room.dto.RoomCreateDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomResponseDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomUpdateDto;

public interface RoomService {

    // 객실 등록
    Long createRoom(Long accommodationsId, RoomCreateDto createDto);


    // 객실 수정
    void updateRoom(Long accommodationsId, Long roomId, RoomUpdateDto updateDto);


    // 객실 삭제
    void deleteRoom(Long accommodationsId, Long roomId);

    // 객실 상세 조회
    RoomResponseDto getRoom(Long accommodationsId, Long roomId);
}
