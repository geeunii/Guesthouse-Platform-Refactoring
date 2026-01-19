package com.ssg9th2team.geharbang.domain.room.controller;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.service.AccommodationService;
import com.ssg9th2team.geharbang.domain.room.dto.RoomCreateDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomUpdateDto;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import com.ssg9th2team.geharbang.domain.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // 객실 등록
    @PostMapping
    public ResponseEntity<?> createRoom(@Valid @RequestBody RoomCreateDto roomCreateDto, Long accommodationId) {
        try {
            roomService.createRoom(accommodationId, roomCreateDto);
            return ResponseEntity.ok().build();
        }catch (Exception e) {
            e.printStackTrace(); // 서버 콘솔에 에러 출력
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }


    // 객실 수정
    @PutMapping("/{accommodationId}/{roomId}")
    public ResponseEntity<?> updateRoom(
            @PathVariable Long accommodationId,
            @PathVariable Long roomId,
            @Valid @RequestBody RoomUpdateDto roomUpdateDto) {
        
        try {
            roomService.updateRoom(accommodationId, roomId, roomUpdateDto);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }


    // 객실 삭제
    @DeleteMapping("/{accommodationId}/{roomId}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long accommodationId,
                                        @PathVariable Long roomId) {
        try {
            roomService.deleteRoom(accommodationId, roomId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message", "객실 삭제 중 오류가 발생했습니다."));
        }
    }

    // 객실 상세 조회
    @GetMapping("/{accommodationId}/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable Long accommodationId,
                                     @PathVariable Long roomId) {
        try {
            return ResponseEntity.ok(roomService.getRoom(accommodationId, roomId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
