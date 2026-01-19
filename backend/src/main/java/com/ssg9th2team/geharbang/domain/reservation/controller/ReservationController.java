package com.ssg9th2team.geharbang.domain.reservation.controller;

import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationRequestDto;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationResponseDto;
import com.ssg9th2team.geharbang.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 예약 생성
     */
    @PostMapping
    public ResponseEntity<?> createReservation(
            @RequestBody ReservationRequestDto requestDto) {
        try {
            System.out.println("DEBUG: Received reservation request: " + requestDto);
            ReservationResponseDto response = reservationService.createReservation(requestDto);
            return ResponseEntity
                    .created(URI.create("/api/reservations/" + response.reservationId()))
                    .body(response);
        } catch (IllegalStateException e) {
            // 정원 초과 등 비즈니스 로직 오류 -> 409 Conflict
            System.err.println("CONFLICT: " + e.getMessage());
            return ResponseEntity.status(409).body(java.util.Map.of("error", "Conflict", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            // 잘못된 요청 파라미터 -> 400 Bad Request
            System.err.println("BAD REQUEST: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Bad Request", "message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("ERROR: Reservation creation failed!");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 예약 단건 조회
     */
    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponseDto> getReservation(
            @PathVariable Long reservationId) {
        ReservationResponseDto response = reservationService.getReservationById(reservationId);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 예약 목록 조회 (관리자용)
     */
    @GetMapping
    public ResponseEntity<List<ReservationResponseDto>> getAllReservations() {
        List<ReservationResponseDto> responses = reservationService.getAllReservations();
        return ResponseEntity.ok(responses);
    }

    /**
     * 사용자별 예약 목록 조회 (URL 파라미터)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsByUser(
            @PathVariable Long userId) {
        List<ReservationResponseDto> responses = reservationService.getReservationsByUserId(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 현재 로그인된 사용자의 예약 목록 조회 (토큰 기반)
     */
    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponseDto>> getMyReservations() {
        List<ReservationResponseDto> responses = reservationService.getMyReservations();
        return ResponseEntity.ok(responses);
    }

    /**
     * 숙소별 예약 목록 조회
     */
    @GetMapping("/accommodation/{accommodationsId}")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsByAccommodation(
            @PathVariable Long accommodationsId) {
        List<ReservationResponseDto> responses = reservationService.getReservationsByAccommodationId(accommodationsId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 대기 상태 예약 삭제 (결제 취소 시)
     */
    @DeleteMapping("/pending/{reservationId}")
    public ResponseEntity<Void> deletePendingReservation(
            @PathVariable Long reservationId) {
        reservationService.deletePendingReservation(reservationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 이용 완료된 예약 삭제 (내역에서 삭제)
     * 체크인 날짜가 지난 확정된 예약만 삭제 가능
     */
    @DeleteMapping("/completed/{reservationId}")
    public ResponseEntity<Void> deleteCompletedReservation(
            @PathVariable Long reservationId) {
        reservationService.deleteCompletedReservation(reservationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 취소된 예약 삭제 (내역에서 삭제)
     */
    @DeleteMapping("/cancelled/{reservationId}")
    public ResponseEntity<Void> deleteCancelledReservation(
            @PathVariable Long reservationId) {
        reservationService.deleteCancelledReservation(reservationId);
        return ResponseEntity.noContent().build();
    }

    // 객실별 예약 목록 조회
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsByRoom(@PathVariable Long roomId) {
        List<ReservationResponseDto> responses = reservationService.getMyReservations();
        return ResponseEntity.ok(responses);
    }
}
