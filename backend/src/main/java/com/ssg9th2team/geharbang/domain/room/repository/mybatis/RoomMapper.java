package com.ssg9th2team.geharbang.domain.room.repository.mybatis;

import com.ssg9th2team.geharbang.domain.room.dto.RoomCreateDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomResponseDto;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoomMapper {

    void insertRoom(@Param("room") Room room);  // 단일 객실 등록 -> 객실 추가시 사용

    void insertRooms(@Param("accommodationsId") Long accommodationsId,  // 다중 객실 등록 -> 숙소 등록할때 사용
                     @Param("rooms") List<RoomCreateDto> rooms);


    void updateRoom(@Param("accommodationsId") Long accommodationsId,  // 수정
                    @Param("roomId") Long roomId,
                    @Param("room") Room room);

    void deleteRoom(@Param("accommodationsId") Long accommodationsId,  // 삭제
                    @Param("roomId") Long roomId);


    RoomResponseDto selectRoomById(@Param("accommodationsId") Long accommodationsId,  // 객실 상세조회
                                   @Param("roomId") Long roomId);
}
