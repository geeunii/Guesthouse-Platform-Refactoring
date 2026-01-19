package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminBookingDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminBookingSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.log.AdminLogConstants;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentRefundJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.service.PaymentService;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final ReservationJpaRepository reservationRepository;
    private final AccommodationJpaRepository accommodationRepository;
    private final UserRepository userRepository;
    private final PaymentJpaRepository paymentRepository;
    private final PaymentRefundJpaRepository refundRepository;
    private final AdminLogService adminLogService;
    private final PaymentService paymentService;

    public AdminPageResponse<AdminBookingSummary> getBookings(
            String status,
            LocalDate from,
            LocalDate to,
            String sort,
            int page,
            int size
    ) {
        Sort sorting = resolveSort(sort);
        List<Reservation> filtered = reservationRepository.findAll(sorting).stream()
                .filter(reservation -> matchesStatus(reservation, status))
                .filter(reservation -> matchesDateRange(reservation, from, to))
                .toList();

        int totalElements = filtered.size();
        int totalPages = size > 0 ? (int) Math.ceil(totalElements / (double) size) : 1;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<AdminBookingSummary> items = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toSummary)
                .toList();
        return AdminPageResponse.of(items, page, size, totalElements, totalPages);
    }

    public AdminBookingDetail getBookingDetail(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
        return toDetail(reservation);
    }

    @Transactional
    public void processRefund(Long adminUserId, Long reservationId, Integer refundAmount, String reason) {
        // 실제 PG사 환불 요청 (PaymentService 호출)
        paymentService.adminRefundPayment(reservationId, reason, refundAmount);

        // 로그 기록
        Map<String, Object> metadata = Map.of(
                "refundAmount", refundAmount,
                "reason", reason
        );
        adminLogService.writeLog(
                adminUserId,
                AdminLogConstants.TARGET_RESERVATION,
                reservationId,
                AdminLogConstants.ACTION_REFUND,
                reason,
                metadata
        );
    }

    private Sort resolveSort(String sort) {
        if (StringUtils.hasText(sort) && sort.equalsIgnoreCase("checkin")) {
            return Sort.by(Sort.Direction.ASC, "checkin");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private Integer parseStatus(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase();
        return switch (normalized) {
            case "pending", "requested" -> 1;
            case "confirmed", "approve" -> 2;
            case "checkedin", "checkin" -> 3;
            case "canceled", "cancelled", "refund" -> 9;
            default -> null;
        };
    }

    private boolean matchesStatus(Reservation reservation, String status) {
        Integer statusCode = parseStatus(status);
        return statusCode == null || statusCode.equals(reservation.getReservationStatus());
    }

    private boolean matchesDateRange(Reservation reservation, LocalDate from, LocalDate to) {
        LocalDateTime checkin = reservation.getCheckin();
        if (checkin == null) {
            return false;
        }
        if (from != null && checkin.isBefore(from.atStartOfDay())) {
            return false;
        }
        if (to != null && !checkin.isBefore(to.plusDays(1).atStartOfDay())) {
            return false;
        }
        return true;
    }

    private AdminBookingSummary toSummary(Reservation reservation) {
        return new AdminBookingSummary(
                reservation.getId(),
                reservation.getAccommodationsId(),
                reservation.getUserId(),
                reservation.getCheckin(),
                reservation.getCheckout(),
                reservation.getGuestCount(),
                reservation.getReservationStatus(),
                reservation.getPaymentStatus(),
                reservation.getFinalPaymentAmount(),
                reservation.getCreatedAt()
        );
    }

    private AdminBookingDetail toDetail(Reservation reservation) {
        String accommodationName = reservation.getAccommodationsId() != null
                ? accommodationRepository.findById(reservation.getAccommodationsId())
                .map(acc -> acc.getAccommodationsName())
                .orElse(null)
                : null;
        Long hostUserId = reservation.getAccommodationsId() != null
                ? accommodationRepository.findById(reservation.getAccommodationsId())
                .map(acc -> acc.getUserId())
                .orElse(null)
                : null;

        User guest = reservation.getUserId() != null
                ? userRepository.findById(reservation.getUserId()).orElse(null)
                : null;

        Payment payment = reservation.getId() != null
                ? paymentRepository.findByReservationId(reservation.getId()).orElse(null)
                : null;

        PaymentRefund refund = null;
        if (payment != null) {
            refund = refundRepository.findByPaymentId(payment.getId()).stream()
                    .max((a, b) -> {
                        if (a.getApprovedAt() != null && b.getApprovedAt() != null) {
                            return a.getApprovedAt().compareTo(b.getApprovedAt());
                        }
                        if (a.getApprovedAt() != null) return 1;
                        if (b.getApprovedAt() != null) return -1;
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    })
                    .orElse(null);
        }

        return new AdminBookingDetail(
                reservation.getId(),
                reservation.getAccommodationsId(),
                accommodationName,
                hostUserId,
                reservation.getUserId(),
                guest != null ? guest.getEmail() : null,
                guest != null ? guest.getPhone() : null,
                reservation.getCheckin(),
                reservation.getCheckout(),
                reservation.getGuestCount(),
                reservation.getReservationStatus(),
                reservation.getPaymentStatus(),
                reservation.getFinalPaymentAmount(),
                payment != null ? payment.getId() : null,
                payment != null ? payment.getOrderId() : null,
                payment != null ? payment.getPgPaymentKey() : null,
                payment != null ? payment.getPaymentMethod() : null,
                payment != null ? payment.getApprovedAmount() : null,
                payment != null ? payment.getApprovedAt() : null,
                refund != null ? refund.getRefundStatus() : null,
                refund != null ? refund.getRefundAmount() : null,
                refund != null ? refund.getApprovedAt() : null,
                refund != null ? refund.getReasonMessage() : null,
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
