package com.ssg9th2team.geharbang.domain.reservation.service;

import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationRequestDto;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationResponseDto;

import java.util.List;

public interface ReservationService {

    /**
     * 예약 생성
     */
    ReservationResponseDto createReservation(ReservationRequestDto requestDto);

    /**
     * 예약 단건 조회
     */
    ReservationResponseDto getReservationById(Long reservationId);

    /**
     * 사용자별 예약 목록 조회
     */
    List<ReservationResponseDto> getReservationsByUserId(Long userId);

    /**
     * 숙소별 예약 목록 조회
     */
    List<ReservationResponseDto> getReservationsByAccommodationId(Long accommodationsId);

    /**
     * 현재 로그인된 사용자의 예약 목록 조회 (토큰 기반)
     */
    List<ReservationResponseDto> getMyReservations();

    /**
     * 대기 상태 예약 삭제 (결제 취소 시)
     */
    void deletePendingReservation(Long reservationId);

    /**
     * 이용 완료된 예약 삭제 (체크인 날짜가 지난 확정된 예약만)
     */
    void deleteCompletedReservation(Long reservationId);

    /**
     * 취소된 예약 삭제 (내역에서 삭제)
     */
    void deleteCancelledReservation(Long reservationId);

    /**
     * 오래된 대기 예약 정리 (스케줄러용)
     */
    int cleanupOldPendingReservations();

    // 객실별 예약 조회
    List<ReservationResponseDto> getReservationByUserId(Long userId);

    /**
     * 전체 예약 목록 조회 (관리자용)
     */
    List<ReservationResponseDto> getAllReservations();
}
