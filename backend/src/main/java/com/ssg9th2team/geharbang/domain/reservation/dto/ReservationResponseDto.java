package com.ssg9th2team.geharbang.domain.reservation.dto;

import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReservationResponseDto(
                Long reservationId,
                Long accommodationsId,
                String accommodationName,
                String accommodationAddress,
                String accommodationImageUrl,
                LocalDateTime checkin,
                LocalDateTime checkout,
                Integer stayNights,
                Integer guestCount,
                Integer reservationStatus,
                Integer totalAmountBeforeDc,
                Integer couponDiscountAmount,
                Integer finalPaymentAmount,
                Integer paymentStatus,
                String reserverName,
                String reserverPhone,
                LocalDateTime createdAt,
                Boolean hasReview,
                String paymentMethod) {

        /**
         * Reservation 엔티티에서 기본 필드만 복사하는 빌더 생성
         */
        public static ReservationResponseDtoBuilder fromReservation(Reservation reservation) {
                return ReservationResponseDto.builder()
                                .reservationId(reservation.getId())
                                .accommodationsId(reservation.getAccommodationsId())
                                .checkin(reservation.getCheckin())
                                .checkout(reservation.getCheckout())
                                .stayNights(reservation.getStayNights())
                                .guestCount(reservation.getGuestCount())
                                .reservationStatus(reservation.getReservationStatus())
                                .totalAmountBeforeDc(reservation.getTotalAmountBeforeDc())
                                .couponDiscountAmount(reservation.getCouponDiscountAmount())
                                .finalPaymentAmount(reservation.getFinalPaymentAmount())
                                .paymentStatus(reservation.getPaymentStatus())
                                .reserverName(reservation.getReserverName())
                                .reserverPhone(reservation.getReserverPhone())
                                .createdAt(reservation.getCreatedAt())
                                .hasReview(false);
        }

        // ===== 하위 호환성을 위한 기존 from 메서드들 (빌더로 위임) =====

        public static ReservationResponseDto from(Reservation reservation) {
                return fromReservation(reservation).build();
        }

        public static ReservationResponseDto from(Reservation reservation, String accommodationName,
                        String accommodationAddress) {
                return fromReservation(reservation)
                                .accommodationName(accommodationName)
                                .accommodationAddress(accommodationAddress)
                                .build();
        }

        public static ReservationResponseDto from(Reservation reservation, String accommodationName,
                        String accommodationAddress, String accommodationImageUrl) {
                return fromReservation(reservation)
                                .accommodationName(accommodationName)
                                .accommodationAddress(accommodationAddress)
                                .accommodationImageUrl(accommodationImageUrl)
                                .build();
        }

        public static ReservationResponseDto from(Reservation reservation, String accommodationName,
                        String accommodationAddress, String accommodationImageUrl, Boolean hasReview) {
                return fromReservation(reservation)
                                .accommodationName(accommodationName)
                                .accommodationAddress(accommodationAddress)
                                .accommodationImageUrl(accommodationImageUrl)
                                .hasReview(hasReview)
                                .build();
        }

        public static ReservationResponseDto from(Reservation reservation, String accommodationName,
                        String accommodationAddress, String accommodationImageUrl, Boolean hasReview,
                        String paymentMethod) {
                return fromReservation(reservation)
                                .accommodationName(accommodationName)
                                .accommodationAddress(accommodationAddress)
                                .accommodationImageUrl(accommodationImageUrl)
                                .hasReview(hasReview)
                                .paymentMethod(paymentMethod)
                                .build();
        }
}
